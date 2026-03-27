from flask import Blueprint, jsonify
from ..database import get_db

word_bp = Blueprint('words', __name__, url_prefix='/words')

@word_bp.route('/', methods=['GET'])
def get_words():
    with get_db() as conn:
        words = conn.execute(
            'SELECT id, german_word, turkish_meaning, example_sentence_de, example_sentence_tr, audio_url FROM words'
        ).fetchall()
    return jsonify([dict(w) for w in words]), 200

@word_bp.route('/<int:word_id>', methods=['GET'])
def get_word(word_id):
    with get_db() as conn:
        word = conn.execute(
            'SELECT id, german_word, turkish_meaning, example_sentence_de, example_sentence_tr, audio_url FROM words WHERE id = ?',
            (word_id,)
        ).fetchone()
    if word is None:
        return jsonify({'error': 'Kelime bulunamadı'}), 404
    return jsonify(dict(word)), 200