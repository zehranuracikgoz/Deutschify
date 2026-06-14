from werkzeug.security import generate_password_hash, check_password_hash
from .database import get_db


def register_user(username, email, password):
    password_hash = generate_password_hash(password)
    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute(
            'INSERT INTO users (username, email, password_hash) VALUES (%s, %s, %s)',
            (username, email, password_hash)
        )

def authenticate_user(email, password):
    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute(
            'SELECT id, username, email, password_hash FROM users WHERE email = %s',
            (email,)
        )
        row = cursor.fetchone()
        if row is None:
            return None
        columns = ['id', 'username', 'email', 'password_hash']
        user = dict(zip(columns, row))
        if check_password_hash(user['password_hash'], password):
            return user
        return None