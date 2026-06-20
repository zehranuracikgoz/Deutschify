import os
from dotenv import load_dotenv
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'), encoding='utf-8-sig')
if not os.environ.get('GITHUB_ACTIONS'):
    os.environ['DATABASE_URL'] = 'postgresql://postgres:postgres@localhost:5433/deutschify'

import pytest
import psycopg2
from backend import create_app


@pytest.fixture
def client():
    app = create_app({'TESTING': True})
    return app.test_client()


@pytest.fixture
def db_conn():
    conn = psycopg2.connect(os.environ['DATABASE_URL'])
    conn.autocommit = False
    yield conn
    conn.close()


@pytest.fixture(autouse=True)
def clean_db():
    yield
    database_url = os.environ.get('DATABASE_URL')
    if not database_url:
        return
    conn = psycopg2.connect(database_url)
    try:
        cursor = conn.cursor()
        cursor.execute("DELETE FROM ai_feedback_logs")
        cursor.execute("DELETE FROM study_sessions")
        cursor.execute("DELETE FROM user_progress")
        cursor.execute("DELETE FROM words")
        cursor.execute("DELETE FROM users")
        cursor.execute("DELETE FROM word_categories")
        cursor.execute("DELETE FROM word_types")
        cursor.execute("DELETE FROM articles")
        conn.commit()
    finally:
        conn.close()