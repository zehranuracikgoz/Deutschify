from datetime import date, datetime, timedelta
from flask import Blueprint, request, jsonify
import psycopg2.extras
from backend.database import get_db
from backend.srs import calculate_next_review
from flask_jwt_extended import jwt_required, get_jwt_identity
import requests
import os

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
            cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
            # next_review_date <=bugün olan kayıtlar icin
            cursor.execute(
                '''
                SELECT w.id AS word_id, w.german_word, w.turkish_meaning,
                       w.example_sentence_de,
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
                       w.example_sentence_de,
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
                        'UPDATE study_sessions SET correct_answers = correct_answers + 1 WHERE id = %s',
                        (session_id,)
                    )
                else:
                    cursor.execute(
                        'UPDATE study_sessions SET wrong_answers = wrong_answers + 1 WHERE id = %s',
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

        session_start = datetime.now().isoformat()

        with get_db() as conn:
            cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
            cursor.execute(
                'INSERT INTO study_sessions (user_id, session_start) VALUES (%s, %s) RETURNING id',
                (user_id, session_start)
            )
            session_id = cursor.fetchone()['id']

        return jsonify({'session_id': session_id, 'message': 'Session started'}), 201

    except Exception as e:
        return jsonify({'error': str(e)}), 500


@study_bp.route('/session/<int:session_id>/end', methods=['PUT'])
def end_session(session_id):
    try:
        session_end = datetime.now().isoformat()

        with get_db() as conn:
            cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
            cursor.execute(
                'SELECT id FROM study_sessions WHERE id = %s',
                (session_id,)
            )
            existing = cursor.fetchone()

            if not existing:
                return jsonify({'error': 'Session bulunamadı'}), 404

            cursor.execute(
                'UPDATE study_sessions SET session_end = %s WHERE id = %s',
                (session_end, session_id)
            )

        return jsonify({'message': 'Session ended'}), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500


@study_bp.route('/example', methods=['POST'])
@jwt_required()
def get_example_sentence():
    data = request.get_json()
    word = data.get('word', '').strip()
    if not word:
        return jsonify({'error': 'word gerekli'}), 400

    api_url = "https://api-inference.huggingface.co/models/zehranuracikgoz/deutschify-t5-small"
    headers = {"Authorization": f"Bearer {os.environ.get('HF_TOKEN')}"}
    payload = {"inputs": f"örnek_üret: {word}"}

    response = requests.post(api_url, headers=headers, json=payload)

    if response.status_code != 200:
        return jsonify({'error': 'Model servisi yanıt vermedi'}), 503

    result = response.json()
    sentence = result[0].get('generated_text', '')

    return jsonify({'word': word, 'example_sentence': sentence}), 200