import sqlite3
import os
from contextlib import contextmanager

DB_PATH = os.path.join(os.path.dirname(__file__), '..', 'instance', 'deutschify.db')

@contextmanager
def get_db():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)

    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    except Exception as e:
        conn.rollback()
        raise e
    finally:
        conn.close()

def init_db():
    with get_db() as conn:
        cursor = conn.cursor()

        # 1. levels tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS levels (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                name     TEXT    NOT NULL UNIQUE,
                min_exp  INTEGER NOT NULL DEFAULT 0
            )
        ''')

        # 2. users tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                username             VARCHAR NOT NULL,
                email                VARCHAR UNIQUE NOT NULL,
                password_hash        VARCHAR NOT NULL,
                daily_streak         INTEGER DEFAULT 0,
                total_xp             INTEGER DEFAULT 0,
                current_level_id     INTEGER,
                preferred_daily_goal INTEGER,
                created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (current_level_id) REFERENCES levels(id)
            )
        ''')

        # 3. articles tablosu (der/die/das)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS articles (
                id   INTEGER PRIMARY KEY AUTOINCREMENT,
                name VARCHAR NOT NULL UNIQUE
            )
        ''')

        # 4. word_categories tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS word_categories (
                id   INTEGER PRIMARY KEY AUTOINCREMENT,
                name VARCHAR NOT NULL UNIQUE
            )
        ''')

        # 5. word_types tablosu (noun, verb, adjective …)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS word_types (
                id   INTEGER PRIMARY KEY AUTOINCREMENT,
                name VARCHAR NOT NULL UNIQUE
            )
        ''')

        # 6. words tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS words (
                id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                german_word         VARCHAR NOT NULL,
                turkish_meaning     VARCHAR NOT NULL,
                example_sentence_de TEXT,
                example_sentence_tr TEXT,
                audio_url           VARCHAR,
                article_id          INTEGER,
                category_id         INTEGER,
                type_id             INTEGER,
                FOREIGN KEY (article_id)  REFERENCES articles(id),
                FOREIGN KEY (category_id) REFERENCES word_categories(id),
                FOREIGN KEY (type_id)     REFERENCES word_types(id)
            )
        ''')

        # 7. study_sessions tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS study_sessions (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id         INTEGER NOT NULL,
                module_type     VARCHAR,
                xp_earned       INTEGER,
                correct_answers INTEGER,
                wrong_answers   INTEGER,
                session_start   TIMESTAMP,
                session_end     TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        ''')

        # 8. user_progress tablosu (spaced-repetition verileri)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS user_progress (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id          INTEGER NOT NULL,
                word_id          INTEGER NOT NULL,
                ease_factor      FLOAT   DEFAULT 2.5,
                interval_days    INTEGER DEFAULT 1,
                repetition_count INTEGER DEFAULT 0,
                last_review_date TIMESTAMP,
                next_review_date TIMESTAMP,
                status           VARCHAR DEFAULT 'new',
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (word_id) REFERENCES words(id)
            )
        ''')

        # 9. ai_feedback_logs tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS ai_feedback_logs (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id          INTEGER NOT NULL,
                word_id          INTEGER NOT NULL,
                wrong_input      TEXT,
                correct_answer   TEXT,
                error_category   VARCHAR,
                ai_analysis_text TEXT,
                feedback_rating  INTEGER,
                created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (word_id) REFERENCES words(id)
            )
        ''')
