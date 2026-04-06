from datetime import date, timedelta
from flask import Blueprint, request, jsonify
from backend.database import get_db
from backend.srs import calculate_next_review

study_bp = Blueprint('study', __name__, url_prefix='/study')


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
            # next_review_date <=bugün olan kayıtlar icin
            due_rows = conn.execute(
                '''
                SELECT w.id AS word_id, w.german_word, w.turkish_meaning,
                       w.example_sentence_de,
                       up.ease_factor, up.interval_days, up.repetition_count, up.status
                FROM user_progress up
                JOIN words w ON w.id = up.word_id
                WHERE up.user_id = ? AND up.next_review_date <= ?
                ''',
                (user_id, today)
            ).fetchall()

            # user_progress kaydı yoksa kelimeler → "new"
            new_rows = conn.execute(
                '''
                SELECT w.id AS word_id, w.german_word, w.turkish_meaning,
                       w.example_sentence_de,
                       2.5 AS ease_factor, 1 AS interval_days,
                       0 AS repetition_count, 'new' AS status
                FROM words w
                WHERE w.id NOT IN (
                    SELECT word_id FROM user_progress WHERE user_id = ?
                )
                ''',
                (user_id,)
            ).fetchall()

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
            return jsonify({'error': 'quality 0 ile 5 arasında bir tam sayı olmalı'}), 400

        today = date.today()

        with get_db() as conn:
            existing = conn.execute(
                'SELECT * FROM user_progress WHERE user_id = ? AND word_id = ?',
                (user_id, word_id)
            ).fetchone()

            if existing:
                ef = existing['ease_factor']
                interval = existing['interval_days']
                rep = existing['repetition_count']
            else:
                ef, interval, rep = 2.5, 1, 0

            result = calculate_next_review(ef, interval, rep, quality)
            next_review = (today + timedelta(days=result['interval_days'])).isoformat()

            if existing:
                conn.execute(
                    '''
                    UPDATE user_progress
                    SET ease_factor = ?, interval_days = ?, repetition_count = ?,
                        status = ?, last_review_date = ?, next_review_date = ?
                    WHERE user_id = ? AND word_id = ?
                    ''',
                    (result['ease_factor'], result['interval_days'], result['repetition_count'],
                     result['status'], today.isoformat(), next_review,
                     user_id, word_id)
                )
            else:
                conn.execute(
                    '''
                    INSERT INTO user_progress
                        (user_id, word_id, ease_factor, interval_days, repetition_count,
                         status, last_review_date, next_review_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ''',
                    (user_id, word_id, result['ease_factor'], result['interval_days'],
                     result['repetition_count'], result['status'],
                     today.isoformat(), next_review)
                )

            xp_earned = 10 if quality >= 3 else 0
            if xp_earned > 0:
                conn.execute(
                    'UPDATE users SET total_xp = total_xp + ? WHERE id = ?',
                    (xp_earned, user_id)
                )

            if session_id:
                if quality >= 3:
                    conn.execute(
                        'UPDATE study_sessions SET correct_answers = correct_answers + 1 WHERE id = ?',
                        (session_id,)
                    )
                else:
                    conn.execute(
                        'UPDATE study_sessions SET wrong_answers = wrong_answers + 1 WHERE id = ?',
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