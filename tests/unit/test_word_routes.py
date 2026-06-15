import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

import pytest
import psycopg2.extras
from backend import create_app
from backend.database import get_db

@pytest.fixture
def seeded_client():
    app = create_app({'TESTING': True})
    with app.app_context():
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("INSERT INTO articles (name) VALUES ('der') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id")
            article_id = cursor.fetchone()[0]
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("INSERT INTO word_categories (name) VALUES ('Temel') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id")
            category_id = cursor.fetchone()[0]
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("INSERT INTO word_types (name) VALUES ('Noun') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id")
            type_id = cursor.fetchone()[0]
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute(
                '''INSERT INTO words
                   (german_word, turkish_meaning, example_sentence_de, article_id, category_id, type_id)
                   VALUES ('Hund', 'köpek', 'Der Hund ist groß.', %s, %s, %s) ON CONFLICT (german_word) DO UPDATE SET german_word=EXCLUDED.german_word RETURNING id''',
                (article_id, category_id, type_id)
            )
            word_id = cursor.fetchone()[0]
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute(
                '''INSERT INTO words
                   (german_word, turkish_meaning, example_sentence_de, article_id, category_id, type_id)
                   VALUES ('Katze', 'kedi', 'Die Katze schläft.', %s, %s, %s) ON CONFLICT DO NOTHING''',
                (article_id, category_id, type_id)
            )
    with app.test_client() as client:
        yield (client, word_id)
    with app.app_context():
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM study_sessions")
            cursor.execute("DELETE FROM user_progress")
            cursor.execute("DELETE FROM words")
            cursor.execute("DELETE FROM users")
            cursor.execute("DELETE FROM word_categories")
            cursor.execute("DELETE FROM word_types")
            cursor.execute("DELETE FROM articles")
            cursor.execute("DELETE FROM levels")

# GET /words/ tüm kelimeleri = 200
def test_get_all_words_returns_200(seeded_client):
    client, word_id = seeded_client
    response = client.get('/words/')
    assert response.status_code == 200
    data = response.get_json()
    assert isinstance(data, list)
    assert len(data) == 2

# GET /words/ responseunda beklenen alanlar olmalı
def test_get_all_words_has_expected_fields(seeded_client):
    client, word_id = seeded_client
    response = client.get('/words/')
    word = response.get_json()[0]
    for field in ('id', 'german_word', 'turkish_meaning', 'example_sentence_de'):
        assert field in word

# GET /words/<id> geçerli id ile = 200 ve doğru kelimeyi döndürmeli
def test_get_word_by_id_returns_200(seeded_client):
    client, word_id = seeded_client
    response = client.get(f'/words/{word_id}')
    assert response.status_code == 200
    data = response.get_json()
    assert data['german_word'] == 'Hund'
    assert data['turkish_meaning'] == 'köpek'

# GET /words/<id> geçersiz id ile =404
def test_get_word_by_invalid_id_returns_404(seeded_client):
    client, word_id = seeded_client
    response = client.get('/words/9999')
    assert response.status_code == 404


@pytest.fixture
def level_seeded_client():
    app = create_app({'TESTING': True})
    with app.app_context():
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("INSERT INTO articles (name) VALUES ('der') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id")
            article_id = cursor.fetchone()[0]
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("INSERT INTO word_categories (name) VALUES ('Temel') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id")
            category_id = cursor.fetchone()[0]
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("INSERT INTO word_types (name) VALUES ('Noun') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id")
            type_id = cursor.fetchone()[0]
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute(
                '''INSERT INTO words
                   (german_word, turkish_meaning, example_sentence_de, article_id, category_id, type_id, level)
                   VALUES ('Hund', 'köpek', 'Der Hund ist groß.', %s, %s, %s, 'A1') ON CONFLICT DO NOTHING''',
                (article_id, category_id, type_id)
            )
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute(
                '''INSERT INTO words
                   (german_word, turkish_meaning, example_sentence_de, article_id, category_id, type_id, level)
                   VALUES ('kaufen', 'satın almak', 'Ich kaufe Brot.', %s, %s, %s, 'A2') ON CONFLICT DO NOTHING''',
                (article_id, category_id, type_id)
            )
    with app.test_client() as client:
        yield client
    with app.app_context():
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM study_sessions")
            cursor.execute("DELETE FROM user_progress")
            cursor.execute("DELETE FROM words")
            cursor.execute("DELETE FROM users")
            cursor.execute("DELETE FROM word_categories")
            cursor.execute("DELETE FROM word_types")
            cursor.execute("DELETE FROM articles")
            cursor.execute("DELETE FROM levels")

# GET /words/?level=A1 -- sadece A1 kelimeleri
def test_get_words_filter_by_level_A1(level_seeded_client):
    response = level_seeded_client.get('/words/?level=A1')
    assert response.status_code == 200
    data = response.get_json()
    assert len(data) >= 1
    assert data[0]['german_word'] == 'Hund'

# GET /words/?level=a1 -- upper() kontrolü, küçük harf de çalışır
def test_get_words_filter_by_level_lowercase(level_seeded_client):
    response = level_seeded_client.get('/words/?level=a1')
    assert response.status_code == 200
    data = response.get_json()
    assert len(data) >= 1
    assert data[0]['german_word'] == 'Hund'