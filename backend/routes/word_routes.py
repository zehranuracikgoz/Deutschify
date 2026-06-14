from flask import Blueprint, jsonify, request
import psycopg2.extras
from ..database import get_db

word_bp = Blueprint('words', __name__, url_prefix='/words')

@word_bp.route('/', methods=['GET'])
def get_words():
    level = request.args.get('level')
    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        if level:
            cursor.execute(
                'SELECT id, german_word, turkish_meaning, example_sentence_de, example_sentence_tr, audio_url FROM words WHERE level = %s',
                (level.upper(),)
            )
        else:
            cursor.execute(
                'SELECT id, german_word, turkish_meaning, example_sentence_de, example_sentence_tr, audio_url FROM words'
            )
        words = cursor.fetchall()
    return jsonify([dict(w) for w in words]), 200

@word_bp.route('/<int:word_id>', methods=['GET'])
def get_word(word_id):
    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cursor.execute(
            'SELECT id, german_word, turkish_meaning, example_sentence_de, example_sentence_tr, audio_url FROM words WHERE id = %s',
            (word_id,)
        )
        word = cursor.fetchone()
    if word is None:
        return jsonify({'error': 'Kelime bulunamadı'}), 404
    return jsonify(dict(word)), 200