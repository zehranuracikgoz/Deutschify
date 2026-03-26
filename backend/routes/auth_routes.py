from flask import Blueprint, request, jsonify
from ..auth import register_user, authenticate_user

auth_bp = Blueprint('auth', __name__, url_prefix='/auth')

@auth_bp.route('/register', methods=['POST'])
def register():
    data = request.get_json()
    if not data:
        return jsonify({'error': 'JSON verisi gerekli'}), 400

    username = data.get('username')
    email = data.get('email')
    password = data.get('password')

    if not all([username, email, password]):
        return jsonify({'error': 'username, email ve password zorunlu'}), 400

    try:
        register_user(username, email, password)
        return jsonify({'message': 'Kayıt başarılı'}), 201
    except Exception as e:
        return jsonify({'error': 'Bu email zaten kayıtlı'}), 409

@auth_bp.route('/login', methods=['POST'])
def login():
    data = request.get_json()
    if not data:
        return jsonify({'error': 'JSON verisi gerekli'}), 400

    email = data.get('email')
    password = data.get('password')

    if not all([email, password]):
        return jsonify({'error': 'email ve password zorunlu'}), 400

    user = authenticate_user(email, password)
    if user:
        return jsonify({'message': 'Giriş başarılı', 'user_id': user['id']}), 200
    return jsonify({'error': 'Geçersiz email veya şifre'}), 401