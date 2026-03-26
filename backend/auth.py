from werkzeug.security import generate_password_hash, check_password_hash
from .database import get_db


def register_user(username, email, password):
    password_hash = generate_password_hash(password)
    with get_db() as conn:
        conn.execute(
            'INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)',
            (username, email, password_hash)
        )

def authenticate_user(email, password):
    with get_db() as conn:
        user = conn.execute(
            'SELECT * FROM users WHERE email = ?', (email,)
        ).fetchone()

    if user and check_password_hash(user['password_hash'], password):
        return dict(user)
    return None