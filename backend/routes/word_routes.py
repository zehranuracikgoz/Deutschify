from flask import Blueprint, jsonify, request
import psycopg2.extras
from ..database import get_db

word_bp = Blueprint('words', __name__, url_prefix='/words')


@word_bp.route('/', methods=['GET'])
def get_words():
    level = request.args.get('level')
    has_article = request.args.get('has_article', '').lower() == 'true'

    conditions = []
    params = []
    if level:
        conditions.append('level = %s')
        params.append(level.upper())
    if has_article:
        conditions.append('article_id IS NOT NULL')

    where_clause = ('WHERE ' + ' AND '.join(conditions)) if conditions else ''
    query = (
        'SELECT id, german_word, turkish_meaning, example_sentence_de, '
        'example_sentence_tr, article_id FROM words ' + where_clause
    )

    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cursor.execute(query, params)
        words = cursor.fetchall()
    return jsonify([dict(w) for w in words]), 200


@word_bp.route('/daily', methods=['GET'])
def get_daily_word():
    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cursor.execute("""
            SELECT id, german_word, turkish_meaning, example_sentence_de, example_sentence_tr
            FROM words
            OFFSET (EXTRACT(DOY FROM NOW())::int % (SELECT COUNT(*) FROM words))
            LIMIT 1
        """)
        word = cursor.fetchone()
    if word is None:
        return jsonify({'error': 'Kelime bulunamadı'}), 404
    return jsonify(dict(word)), 200


@word_bp.route('/<int:word_id>', methods=['GET'])
def get_word(word_id):
    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cursor.execute(
            'SELECT id, german_word, turkish_meaning, example_sentence_de, example_sentence_tr FROM words WHERE id = %s',
            (word_id,)
        )
        word = cursor.fetchone()
    if word is None:
        return jsonify({'error': 'Kelime bulunamadı'}), 404
    return jsonify(dict(word)), 200