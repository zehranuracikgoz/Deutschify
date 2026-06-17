import io
from flask import Blueprint, send_file, jsonify
from flask_jwt_extended import jwt_required
from gtts import gTTS

tts_bp = Blueprint('tts', __name__, url_prefix='/tts')


@tts_bp.route('/<word>', methods=['GET'])
@jwt_required()
def pronounce(word):
    try:
        tts = gTTS(text=word, lang='de')
        buf =io.BytesIO()
        tts.write_to_fp(buf)
        buf.seek(0)
        return send_file(buf, mimetype='audio/mpeg', as_attachment=False)
    except Exception as e:
        return jsonify({'error': 'TTS hatası', 'detail': str(e)}), 503