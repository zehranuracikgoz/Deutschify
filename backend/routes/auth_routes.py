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
        cursor = conn.cursor()
        cursor.execute("""
            SELECT u.id, u.username, u.email, u.total_xp,
                   u.daily_streak, u.preferred_daily_goal, u.created_at,
                   l.name as level_name
            FROM users u
            LEFT JOIN levels l ON u.current_level_id = l.id
            WHERE u.id = %s
        """, (user_id,))
        user = cursor.fetchone()
        if user is None:
            return jsonify({'error': 'Kullanıcı bulunamadı'}), 401
        columns = [desc[0] for desc in cursor.description]
        user_dict = dict(zip(columns, user))

        cursor.execute("""
            SELECT COUNT(*) as total_sessions,
                   COALESCE(SUM(correct_answers), 0) as total_correct
            FROM study_sessions
            WHERE user_id = %s
        """, (user_id,))
        stats = cursor.fetchone()

    return jsonify({
        'id': user_dict['id'],
        'username': user_dict['username'],
        'email':  user_dict['email'],
        'total_xp': user_dict['total_xp'],
        'daily_streak':user_dict['daily_streak'],
        'preferred_daily_goal': user_dict['preferred_daily_goal'],
        'created_at':str(user_dict['created_at']),
        'level_name': user_dict['level_name'],
        'total_sessions': stats[0] if stats else 0,
        'total_correct': stats[1] if stats else 0,
    }), 200