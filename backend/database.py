import os
import psycopg2
from contextlib import contextmanager

@contextmanager
def get_db():
    database_url = os.environ.get('DATABASE_URL')
    if not database_url:
        raise RuntimeError('DATABASE_URL environment variable is not set')
    conn = psycopg2.connect(database_url)
    conn.autocommit = False
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
                id       SERIAL PRIMARY KEY,
                name     TEXT    NOT NULL UNIQUE,
                min_exp  INTEGER NOT NULL DEFAULT 0
            )
        ''')

        # 2. users tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id                   SERIAL PRIMARY KEY,
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
                id   SERIAL PRIMARY KEY,
                name VARCHAR NOT NULL UNIQUE
            )
        ''')

        # 4. word_categories tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS word_categories (
                id   SERIAL PRIMARY KEY,
                name VARCHAR NOT NULL UNIQUE
            )
        ''')

        # 5. word_types tablosu (noun, verb, adjective …)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS word_types (
                id   SERIAL PRIMARY KEY,
                name VARCHAR NOT NULL UNIQUE
            )
        ''')

        # 6. words tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS words (
                id                  SERIAL PRIMARY KEY,
                german_word         VARCHAR NOT NULL UNIQUE,
                turkish_meaning     VARCHAR NOT NULL,
                example_sentence_de TEXT,
                example_sentence_tr TEXT,
                article_id          INTEGER,
                category_id         INTEGER,
                type_id             INTEGER,
                level               VARCHAR DEFAULT 'A1',
                FOREIGN KEY (article_id)  REFERENCES articles(id),
                FOREIGN KEY (category_id) REFERENCES word_categories(id),
                FOREIGN KEY (type_id)     REFERENCES word_types(id)
            )
        ''')

        # 7. study_sessions tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS study_sessions (
                id              SERIAL PRIMARY KEY,
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
                id               SERIAL PRIMARY KEY,
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
                id               SERIAL PRIMARY KEY,
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

        cursor.execute('''
            ALTER TABLE study_sessions
                ADD COLUMN IF NOT EXISTS module_type VARCHAR,
                ADD COLUMN IF NOT EXISTS xp_earned   INTEGER DEFAULT 0
        ''')

        cursor.execute('''
            UPDATE study_sessions
            SET correct_answers = 0 WHERE correct_answers IS NULL
        ''')
        cursor.execute('''
            UPDATE study_sessions
            SET wrong_answers = 0 WHERE wrong_answers IS NULL
        ''')
        cursor.execute('''
            ALTER TABLE study_sessions
                ADD COLUMN IF NOT EXISTS max_streak INTEGER DEFAULT  0
        ''')

        cursor.execute('''
            ALTER TABLE ai_feedback_logs
                ADD COLUMN IF NOT EXISTS word TEXT,
                ADD COLUMN IF NOT EXISTS user_answer TEXT,
                ADD COLUMN IF NOT EXISTS feedback_text TEXT
        ''')

        # 10. grammar_topics tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS grammar_topics (
                id            SERIAL PRIMARY KEY,
                title         VARCHAR NOT NULL,
                slug          VARCHAR NOT NULL UNIQUE,
                level         VARCHAR NOT NULL,
                explanation   TEXT,
                display_order INTEGER DEFAULT 0
            )
        ''')

        # 11. grammar_exercises tablosu
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS grammar_exercises (
                id             SERIAL PRIMARY KEY,
                topic_id       INTEGER NOT NULL REFERENCES grammar_topics(id) ON DELETE CASCADE,
                question       TEXT NOT NULL,
                exercise_type  VARCHAR NOT NULL,
                options        JSONB,
                correct_answer TEXT NOT NULL,
                explanation    TEXT,
                display_order  INTEGER DEFAULT 0
            )
        ''')

        cursor.execute('''
            CREATE INDEX IF NOT EXISTS idx_grammar_exercises_topic_id
            ON grammar_exercises(topic_id)
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS user_grammar_progress (
                id SERIAL PRIMARY KEY,
                user_id INTEGER NOT NULL REFERENCES users(id),
                exercise_id INTEGER NOT NULL REFERENCES grammar_exercises(id),
                ease_factor FLOAT   DEFAULT 2.5,
                interval_days INTEGER DEFAULT 0,
                repetition_count INTEGER DEFAULT 0,
                next_review_date DATE DEFAULT CURRENT_DATE,
                last_review_date DATE,
                UNIQUE(user_id, exercise_id)
            )
        ''')