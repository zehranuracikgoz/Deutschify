from datetime import date, datetime, timedelta, timezone

TZ_TR = timezone(timedelta(hours=3))
from flask import Blueprint, request, jsonify
import psycopg2.extras
from backend.database import get_db
from backend.srs import calculate_next_review
from flask_jwt_extended import jwt_required, get_jwt_identity
import requests
import os
import google.generativeai as genai

study_bp = Blueprint('study', __name__, url_prefix='/study')

_t5_tokenizer = None
_t5_model = None


@study_bp.route('/queue/<int:user_id>', methods=['GET'])
def get_queue(user_id):
    try:
        today = date.today().isoformat()

        try:
            limit = int(request.args.get('limit', 10))
            if limit < 1:
                return jsonify({'error': 'limit en az 1 olmalı'}), 400
        except ValueError:
            return jsonify({'error': 'limit geçerli bir tam sayı olmalı'}), 400

        with get_db() as conn:
            cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
            # next_review_date <=bugün olan kayıtlar icin
            cursor.execute(
                '''
                SELECT w.id AS word_id, w.german_word, w.turkish_meaning,
                       w.example_sentence_de, w.example_sentence_tr,
                       up.ease_factor, up.interval_days, up.repetition_count, up.status
                FROM user_progress up
                JOIN words w ON w.id = up.word_id
                WHERE up.user_id = %s AND up.next_review_date <= %s
                ''',
                (user_id, today)
            )
            due_rows = cursor.fetchall()

            # user_progress kaydı yoksa kelimeler → "new"
            cursor.execute(
                '''
                SELECT w.id AS word_id, w.german_word, w.turkish_meaning,
                       w.example_sentence_de, w.example_sentence_tr,
                       2.5 AS ease_factor, 1 AS interval_days,
                       0 AS repetition_count, 'new' AS status
                FROM words w
                WHERE w.id NOT IN (
                    SELECT word_id FROM user_progress WHERE user_id = %s
                )
                ''',
                (user_id,)
            )
            new_rows = cursor.fetchall()

        queue = [dict(r) for r in due_rows] + [dict(r) for r in new_rows]
        queue = queue[:limit]

        if not queue:
            return jsonify({'queue': [], 'message': 'Bugün çalışılacak kelime yok'}), 200

        return jsonify({'queue': queue}), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500


@study_bp.route('/answer', methods=['POST'])
def submit_answer():
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'JSON verisi gerekli'}), 400

        user_id = data.get('user_id')
        word_id = data.get('word_id')
        quality = data.get('quality')
        session_id = data.get('session_id')

        if any(v is None for v in [user_id, word_id, quality]):
            return jsonify({'error': 'user_id, word_id ve quality zorunlu'}), 400

        if not isinstance(quality, int) or quality < 1 or quality > 5:
            return jsonify({'error': 'quality 1 ile 5 arasında bir tam sayı olmalı'}), 400

        today = date.today()

        with get_db() as conn:
            cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
            cursor.execute(
                'SELECT * FROM user_progress WHERE user_id = %s AND word_id = %s',
                (user_id, word_id)
            )
            existing = cursor.fetchone()

            if existing:
                ef = existing['ease_factor']
                interval = existing['interval_days']
                rep = existing['repetition_count']
            else:
                ef, interval, rep = 2.5, 1, 0

            result = calculate_next_review(ef, interval, rep, quality)
            next_review = (today + timedelta(days=result['interval_days'])).isoformat()

            if existing:
                cursor.execute(
                    '''
                    UPDATE user_progress
                    SET ease_factor = %s, interval_days = %s, repetition_count = %s,
                        status = %s, last_review_date = %s, next_review_date = %s
                    WHERE user_id = %s AND word_id = %s
                    ''',
                    (result['ease_factor'], result['interval_days'], result['repetition_count'],
                     result['status'], today.isoformat(), next_review,
                     user_id, word_id)
                )
            else:
                cursor.execute(
                    '''
                    INSERT INTO user_progress
                        (user_id, word_id, ease_factor, interval_days, repetition_count,
                         status, last_review_date, next_review_date)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                    ''',
                    (user_id, word_id, result['ease_factor'], result['interval_days'],
                     result['repetition_count'], result['status'],
                     today.isoformat(), next_review)
                )

            xp_earned = 10 if quality >= 3 else 0
            if xp_earned > 0:
                cursor.execute(
                    'UPDATE users SET total_xp = total_xp + %s WHERE id = %s',
                    (xp_earned, user_id)
                )

            if session_id:
                if quality >= 3:
                    cursor.execute(
                        '''UPDATE study_sessions
                           SET correct_answers = COALESCE(correct_answers, 0) + 1,
                               xp_earned       = COALESCE(xp_earned, 0) + %s
                           WHERE id = %s''',
                        (xp_earned, session_id)
                    )
                else:
                    cursor.execute(
                        '''UPDATE study_sessions
                           SET wrong_answers = COALESCE(wrong_answers, 0) + 1
                           WHERE id = %s''',
                        (session_id,)
                    )

        return jsonify({
            'message': 'ok',
            'next_review_days': result['interval_days'],
            'xp_earned': xp_earned,
            'status': result['status'],
        }), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500


@study_bp.route('/session/start', methods=['POST'])
def start_session():
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'JSON verisi gerekli'}), 400

        user_id = data.get('user_id')
        if user_id is None:
            return jsonify({'error': 'user_id zorunlu'}), 400

        module_type   = data.get('module_type')
        session_start = datetime.now(timezone.utc).isoformat()

        with get_db() as conn:
            cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
            cursor.execute(
                'INSERT INTO study_sessions (user_id, module_type, session_start) VALUES (%s, %s, %s) RETURNING id',
                (user_id, module_type, session_start)
            )
            session_id = cursor.fetchone()['id']

        return jsonify({'session_id': session_id, 'message': 'Session started'}), 201

    except Exception as e:
        return jsonify({'error': str(e)}), 500


@study_bp.route('/session/<int:session_id>/end', methods=['PUT'])
def end_session(session_id):
    try:
        session_end = datetime.now(timezone.utc).isoformat()
        data = request.get_json(silent=True) or {}
        max_streak = data.get('max_streak')

        with get_db() as conn:
            cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
            cursor.execute(
                'SELECT id, user_id FROM study_sessions WHERE id = %s',
                (session_id,)
            )
            existing = cursor.fetchone()

            if not existing:
                return jsonify({'error': 'Session bulunamadı'}), 404

            user_id = existing['user_id']

            if max_streak is not None:
                cursor.execute(
                    'UPDATE study_sessions SET session_end = %s, max_streak = %s WHERE id = %s',
                    (session_end, int(max_streak), session_id )
                )
            else:
                cursor.execute(
                    'UPDATE study_sessions SET session_end=%s WHERE id = %s',
                    (session_end, session_id)
                )

            now_tr = datetime.now(TZ_TR)
            today_tr = now_tr.date().isoformat()
            yesterday_tr = (now_tr.date() - timedelta(days=1)).isoformat()

            cursor.execute(
                '''
                SELECT COUNT(*) FROM study_sessions
                WHERE user_id = %s
                  AND DATE(session_start AT TIME ZONE 'Europe/Istanbul') = %s
                  AND id != %s
                  AND session_end IS NOT NULL
                ''',
                (user_id, today_tr, session_id)
            )
            today_count = cursor.fetchone()['count']

            if today_count == 0:
                cursor.execute(
                    '''
                    SELECT COUNT(*) FROM study_sessions
                    WHERE user_id = %s
                      AND DATE(session_start AT TIME ZONE 'Europe/Istanbul') = %s
                      AND session_end IS NOT NULL
                    ''',
                    (user_id, yesterday_tr)
                )
                yesterday_count = cursor.fetchone()['count']

                if yesterday_count > 0:
                    cursor.execute(
                        'UPDATE users SET daily_streak = daily_streak + 1 WHERE id = %s',
                        (user_id,)
                    )
                else:
                    cursor.execute(
                        'UPDATE users SET daily_streak = 1 WHERE id = %s',
                        (user_id,)
                    )

        return jsonify({'message': 'Session ended'}), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500


@study_bp.route('/example', methods=['POST'])
@jwt_required()
def get_example_sentence():
    global _t5_tokenizer, _t5_model
    data = request.get_json()
    word = data.get('word', '').strip()
    if not word:
        return jsonify({'error': 'word gerekli'}), 400

    # 1 - lokal T5 inference — sadece USE_LOCAL_T5=true ise etkin
    if os.environ.get('USE_LOCAL_T5') == 'true':
        try:
            from transformers import T5Tokenizer, T5ForConditionalGeneration
            import torch
            if _t5_tokenizer is None or _t5_model is None:
                _t5_tokenizer = T5Tokenizer.from_pretrained('zehranuracikgoz/deutschify-t5-small')
                _t5_model = T5ForConditionalGeneration.from_pretrained('zehranuracikgoz/deutschify-t5-small')
                _t5_model.eval()
            inp = _t5_tokenizer(
                f'generate sentence: {word}',
                return_tensors='pt', max_length=64, truncation=True
            )
            with torch.no_grad():
                out = _t5_model.generate(
                    inp['input_ids'], max_new_tokens=60, num_beams=4, early_stopping=True
                )
            sentence = _t5_tokenizer.decode(out[0], skip_special_tokens=True)
            if sentence:
                return jsonify({'word': word, 'example_sentence': sentence}), 200
        except ImportError:
            pass  # transformers kurulu değilse HF API'ye düş

    # 2- HF Inference API fallback
    try:
        api_url = "https://api-inference.huggingface.co/models/zehranuracikgoz/deutschify-t5-small"
        headers ={"Authorization": f"Bearer {os.environ.get('HF_TOKEN')}"}
        payload = {"inputs": f"örnek_üret: {word}"}
        response =requests.post(api_url, headers=headers, json=payload, timeout=10)
        if response.status_code==200:
            result = response.json()
            sentence = result[0].get('generated_text', '')
            if sentence:
                return jsonify({'word': word, 'example_sentence': sentence}), 200
    except Exception:
        pass

    # 3- Statik fallback — DB deki example_sentence_de
    try:
        with get_db() as conn:
            cur = conn.cursor()
            cur.execute(
                'SELECT example_sentence_de FROM words WHERE german_word ILIKE %s LIMIT 1',
                (word,)
            )
            row = cur.fetchone()
            if row and row[0]:
                return jsonify({'word': word, 'example_sentence': row[0]}), 200
    except Exception:
        pass

    return jsonify({'error': 'Örnek cümle üretilemedi'}), 503


@study_bp.route('/feedback', methods=['POST'])
@jwt_required()
def get_ai_feedback():
    user_id = int(get_jwt_identity())
    data = request.get_json()
    word= data.get('word', '').strip()
    user_answer = data.get('user_answer', '').strip()
    correct_answer =data.get('correct_answer', '').strip()

    if not all([word, user_answer, correct_answer]):
        return jsonify({'error': 'word, user_answer, correct_answer gerekli'}), 400

    try:
        genai.configure(api_key=os.environ.get('GEMINI_API_KEY'))
        model =genai.GenerativeModel('gemini-2.5-flash-lite')

        prompt = (
            f"Bir Almanca öğrencisi '{word}' kelimesine '{user_answer}' cevabını verdi, "
            f"doğru cevap '{correct_answer}' idi. Bu öğrenciye Türkçe, kısa (2-3 cümle), "
            f"A1 seviyesine uygun, neden yanlış olduğunu açıklayan dostça bir gramer "
            f"geri bildirimi ver. Teknik terim kullanma."
        )

        response = model.generate_content(prompt)
        feedback_text =response.text

        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT id FROM words WHERE german_word = %s", (word,))
            word_row = cursor.fetchone()
            word_id = word_row[0] if word_row else None

            if word_id is not None:
                cursor.execute("""
                    INSERT INTO ai_feedback_logs (user_id, word_id, word, user_answer, correct_answer, feedback_text, created_at)
                    VALUES (%s, %s, %s, %s, %s, %s, NOW())
                """, (user_id,word_id, word, user_answer, correct_answer, feedback_text))
                conn.commit()

        return jsonify({'feedback': feedback_text}), 200

    except Exception as e:
        return jsonify({'error': 'AI servisi şu anda kullanılamıyor', 'detail': str(e)}), 503


@study_bp.route('/history', methods=['GET'])
@jwt_required()
def get_history():
    user_id = int(get_jwt_identity())
    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, module_type, session_start, xp_earned, correct_answers, wrong_answers
            FROM study_sessions
            WHERE user_id = %s
              AND session_start >= NOW() - INTERVAL '30 days'
            ORDER BY session_start DESC
            LIMIT 100
        """, (user_id,))
        rows = cursor.fetchall()

    def calc_rate(correct, wrong):
        total = (correct or 0) + (wrong or 0)
        return round((correct / total) *100) if total > 0 else None

    def to_tr(dt):
        if dt is None:
            return None
        if dt.tzinfo is None:
            dt =dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(TZ_TR)

    result = [
        {
            "session_id": row[0],
            "module_type": row[1] or 'flashcard',
            "date": to_tr(row[2]).strftime('%Y-%m-%d') if row[2] else '',
            "time": to_tr(row[2]).strftime('%H:%M') if row[2] else '',
            "xp_earned": row[3] or 0,
            "correct": row[4] or 0,
            "wrong": row[5] or 0,
            "success_rate": calc_rate(row[4], row[5])
        }
        for row in rows
    ]
    return jsonify({"sessions": result}), 200


@study_bp.route('/review', methods=['GET'])
@jwt_required()
def get_review_words():
    user_id = int(get_jwt_identity())
    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT w.id, w.german_word, w.turkish_meaning,
                   w.example_sentence_de, w.example_sentence_tr,
                   up.ease_factor, up.repetition_count, up.next_review_date
            FROM user_progress up
            JOIN words w ON up.word_id = w.id
            WHERE up.user_id = %s
              AND (up.ease_factor <2.5 OR up.next_review_date < CURRENT_DATE)
            ORDER BY up.ease_factor ASC, up.next_review_date ASC
            LIMIT 20
        """, (user_id,))
        rows = cursor.fetchall()

    if not rows:
        return jsonify({'words': [], 'message': 'Henüz tekrar edilecek kelime yok'}), 200

    result = [{
        'id': row[0],
        'german_word': row[1],
        'turkish_meaning': row[2],
        'example_sentence_de': row[3],
        'example_sentence_tr': row[4],
        'ease_factor': round(float(row[5]), 2),
        'repetitions':row[6],
        'next_review_date':str(row[7]) if row[7] else None
    } for row in rows]

    return jsonify({'words': result}), 200


@study_bp.route('/stats', methods=['GET'])
@jwt_required()
def get_stats():
    try:
        user_id = int(get_jwt_identity())
        with get_db() as conn:
            cursor = conn.cursor()

            # Toplam XP ve streak
            cursor.execute("""
                SELECT total_xp, daily_streak
                FROM users
                WHERE id = %s
            """, (user_id,))
            user = cursor.fetchone()

            # Son 7 günlük oturum sayısı
            cursor.execute("""
                SELECT DATE(session_start) as day, COUNT(*) as count
                FROM study_sessions
                WHERE user_id = %s
                  AND session_start >= NOW() - INTERVAL '7 days'
                GROUP BY DATE(session_start)
                ORDER BY day ASC
            """, (user_id,))
            sessions = cursor.fetchall()

            # Son 7 günlük çalışma süresi (dakika)
            cursor.execute("""
                SELECT DATE(session_start) as day,
                       COALESCE(SUM(
                           CASE
                               WHEN session_end IS NOT NULL THEN
                                   LEAST(EXTRACT(EPOCH FROM (session_end - session_start)) / 60, 120)
                               ELSE 5
                           END
                       ), 0) as minutes
                FROM study_sessions
                WHERE user_id = %s
                  AND session_start >= NOW() - INTERVAL '7 days'
                GROUP BY DATE(session_start)
                ORDER BY day ASC
            """, (user_id,))
            durations = cursor.fetchall()

            # Toplam doğru ve yanlış
            cursor.execute("""
                SELECT
                    COALESCE(SUM(correct_answers), 0) as total_correct,
                    COALESCE(SUM(wrong_answers), 0) as total_wrong
                FROM study_sessions
                WHERE user_id = %s
            """, (user_id,))
            totals = cursor.fetchone()

            # En uzun doğru serisi (artikel + grammar)
            try:
                cursor.execute("""
                    SELECT COALESCE(MAX(max_streak), 0)
                    FROM study_sessions
                    WHERE user_id = %s AND module_type IN ('artikel', 'grammar')
                """, (user_id,))
                max_streak_row=cursor.fetchone()
                max_streak =int(max_streak_row[0]) if max_streak_row else 0
            except Exception:
                conn.rollback()
                max_streak = 0

        # Son 7 günü doldur (veri olmayan günler 0)
        from datetime import datetime, timedelta
        today = datetime.utcnow().date()
        days = [(today - timedelta(days=6-i)) for i in range(7)]
        session_map = {row[0]: row[1] for row in sessions}
        weekly = [session_map.get(day, 0) for day in days]

        duration_map = {row[0]: float(row[1] or 0) for row in durations}
        weekly_minutes = [round(duration_map.get(day, 0)) for day in days]

        total_correct = totals[0] or 0
        total_wrong = totals[1] or 0
        total_answers = total_correct + total_wrong
        success_rate = round((total_correct / total_answers) * 100) if total_answers > 0 else 0

        return jsonify({
            'total_xp':        user[0] if user else 0,
            'daily_streak':    user[1] if user else 0,
            'weekly_sessions': weekly,
            'weekly_minutes':  weekly_minutes,
            'total_correct':   total_correct,
            'total_wrong':     total_wrong,
            'success_rate':    success_rate,
            'max_streak':      max_streak
        }), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500