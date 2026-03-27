import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

from backend.database import init_db, get_db

# init_db() çalıştırıldığında tüm tabloların doğrulanması - şemadaki değişikliklerde tablonun eksik olmaması
def test_tables_exist():
    init_db()
    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = {row['name'] for row in cursor.fetchall()}

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
        cursor = conn.cursor()
        cursor.execute("PRAGMA table_info(user_progress)")
        columns = {row['name'] for row in cursor.fetchall()}
    assert 'word_id' in columns

# daily_streak'in 0dan başlaması - streak mantığının doğrulanması
def test_streak_starts_at_zero():
    init_db()
    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute("PRAGMA table_info(users)")
        columns = {row['name']: row for row in cursor.fetchall()}
    assert columns['daily_streak']['dflt_value'] == '0'