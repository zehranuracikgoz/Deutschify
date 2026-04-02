from flask import Blueprint, request, jsonify
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity
from ..auth import register_user, authenticate_user
from ..database import get_db

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
        access_token = create_access_token(identity=str(user['id']))
        return jsonify({
            'access_token': access_token,
            'user_id': user['id'],
            'username': user['username'],
        }), 200
    return jsonify({'error': 'Geçersiz email veya şifre'}), 401

@auth_bp.route('/me', methods=['GET'])
@jwt_required()
def me():
    user_id = int(get_jwt_identity())
    with get_db() as conn:
        user = conn.execute(
            'SELECT id, username, email, total_xp FROM users WHERE id = ?',
            (user_id,)
        ).fetchone()
    if user is None:
        return jsonify({'error': 'Kullanıcı bulunamadı'}), 401
    return jsonify(dict(user)), 200