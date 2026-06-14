from werkzeug.security import generate_password_hash, check_password_hash
import psycopg2.extras
from .database import get_db


def register_user(username, email, password):
    password_hash = generate_password_hash(password)
    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cursor.execute(
            'INSERT INTO users (username, email, password_hash) VALUES (%s, %s, %s)',
            (username, email, password_hash)
        )

def authenticate_user(email, password):
    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cursor.execute(
            'SELECT * FROM users WHERE email = %s', (email,)
        )
        user = cursor.fetchone()

    if user and check_password_hash(user['password_hash'], password):
        return dict(user)
    return None