import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

import psycopg2.extras
from backend.database import init_db, get_db

# init_db() çalıştırıldığında tüm tabloların doğrulanması - şemadaki değişikliklerde tablonun eksik olmaması
def test_tables_exist():
    init_db()
    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cursor.execute("SELECT table_name FROM information_schema.tables WHERE table_schema='public'")
        tables = {row['table_name'] for row in cursor.fetchall()}

    expected = {
        'levels', 'users', 'articles', 'word_categories',
        'word_types', 'words', 'study_sessions',
        'user_progress', 'ai_feedback_logs'
    }
    assert expected.issubset(tables)

# user_progress tablosunun word_id sütununu içermesi - kelime bazlı ilerleme için
def test_word_id_in_user_progress():
    init_db()
    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cursor.execute("SELECT column_name FROM information_schema.columns WHERE table_name='user_progress'")
        columns = {row['column_name'] for row in cursor.fetchall()}
    assert 'word_id' in columns

# daily_streak'in 0dan başlaması - streak mantığının doğrulanması
def test_streak_starts_at_zero():
    init_db()
    with get_db() as conn:
        cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cursor.execute("SELECT column_name, column_default FROM information_schema.columns WHERE table_name='users'")
        columns = {row['column_name']: row for row in cursor.fetchall()}
    assert columns['daily_streak']['column_default'] == '0'