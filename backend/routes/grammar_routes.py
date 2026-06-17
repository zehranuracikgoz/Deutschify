from flask import Blueprint, request, jsonify
import psycopg2.extras
from backend.database import get_db

grammar_bp = Blueprint('grammar', __name__, url_prefix='/grammar')


@grammar_bp.route('/topics',methods=['GET'])
def get_topics():
    level = request.args.get('level')
    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        if level:
            cursor.execute(
                'SELECT id, title, slug, level, display_order FROM grammar_topics '
                'WHERE level = %s ORDER BY display_order',
                (level.upper(),)
            )
        else:
            cursor.execute(
                'SELECT id, title, slug, level, display_order FROM grammar_topics '
                'ORDER BY display_order'
            )
        topics = cursor.fetchall()
    return jsonify([dict(t) for t in topics])


@grammar_bp.route('/topics/<slug>/exercises', methods=['GET'])
def get_topic_exercises(slug):
    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cursor.execute(
            'SELECT id, title, slug, level, explanation FROM grammar_topics WHERE slug = %s',
            (slug,)
        )
        topic = cursor.fetchone()
        if not topic:
            return jsonify({'error':'Konu bulunamadı'}), 404

        cursor.execute(
            'SELECT id, question, exercise_type, options, display_order '
            'FROM grammar_exercises WHERE topic_id = %s ORDER BY display_order',
            (topic['id'],)
        )
        exercises = cursor.fetchall()

    result = dict(topic)
    result['exercises'] = [dict(e) for e in exercises]
    return jsonify(result)


@grammar_bp.route('/check', methods=['POST'])
def check_answer():
    data = request.get_json()
    if not data or 'exercise_id' not in data or 'answer' not in data:
        return jsonify({'error':'exercise_id ve answer alanları gerekli'}), 400

    exercise_id = data['exercise_id']
    user_answer = str(data['answer']).strip().lower()

    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cursor.execute(
            'SELECT correct_answer , explanation FROM grammar_exercises WHERE id = %s',
            (exercise_id,)
        )
        exercise = cursor.fetchone()

    if not exercise:
        return jsonify({'error': 'Soru bulunamadı'}), 404

    correct = user_answer== exercise['correct_answer'].strip().lower()
    xp_earned = 0

    if correct:
        xp_earned = 5
        user_id = data.get('user_id')
        if user_id:
            try:
                with get_db() as conn:
                    cursor =conn.cursor()
                    cursor.execute(
                        'UPDATE users SET total_xp = total_xp + %s WHERE id = %s',
                        (xp_earned, int(user_id))
                    )
            except Exception:
                xp_earned = 5

    return jsonify({
        'correct': correct,
        'correct_answer': exercise['correct_answer'],
        'explanation' : exercise['explanation'],
        'xp_earned': xp_earned
    })