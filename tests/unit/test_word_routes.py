import sys
import os
import tempfile

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

import pytest
from backend import create_app
from backend.database import get_db

@pytest.fixture
def seeded_client():
    db_fd, db_path = tempfile.mkstemp()
    app = create_app({'TESTING': True, 'DATABASE': db_path})
    with app.app_context():
        with get_db() as conn:
            conn.execute("INSERT INTO articles (name) VALUES ('der')")
            conn.execute("INSERT INTO word_categories (name) VALUES ('Temel')")
            conn.execute("INSERT INTO word_types (name) VALUES ('Noun')")
            conn.execute(
                '''INSERT INTO words
                   (german_word, turkish_meaning, example_sentence_de, article_id, category_id, type_id)
                   VALUES ('Hund', 'köpek', 'Der Hund ist groß.', 1, 1, 1)'''
            )
            conn.execute(
                '''INSERT INTO words
                   (german_word, turkish_meaning, example_sentence_de, article_id, category_id, type_id)
                   VALUES ('Katze', 'kedi', 'Die Katze schläft.', 1, 1, 1)'''
            )
    with app.test_client() as client:
        yield client
    os.close(db_fd)
    os.unlink(db_path)

# GET /words/ tüm kelimeleri = 200
def test_get_all_words_returns_200(seeded_client):
    response = seeded_client.get('/words/')
    assert response.status_code == 200
    data = response.get_json()
    assert isinstance(data, list)
    assert len(data) == 2

# GET /words/ responseunda beklenen alanlar olmalı
def test_get_all_words_has_expected_fields(seeded_client):
    response = seeded_client.get('/words/')
    word = response.get_json()[0]
    for field in ('id', 'german_word', 'turkish_meaning', 'example_sentence_de'):
        assert field in word

# GET /words/<id> geçerli id ile = 200 ve doğru kelimeyi döndürmeli
def test_get_word_by_id_returns_200(seeded_client):
    response = seeded_client.get('/words/1')
    assert response.status_code == 200
    data = response.get_json()
    assert data['german_word'] == 'Hund'
    assert data['turkish_meaning'] == 'köpek'

# GET /words/<id> geçersiz id ile =404
def test_get_word_by_invalid_id_returns_404(seeded_client):
    response = seeded_client.get('/words/9999')
    assert response.status_code == 404


@pytest.fixture
def level_seeded_client():
    db_fd, db_path = tempfile.mkstemp()
    app = create_app({'TESTING': True, 'DATABASE': db_path})
    with app.app_context():
        with get_db() as conn:
            conn.execute("INSERT INTO articles (name) VALUES ('der')")
            conn.execute("INSERT INTO word_categories (name) VALUES ('Temel')")
            conn.execute("INSERT INTO word_types (name) VALUES ('Noun')")
            conn.execute(
                '''INSERT INTO words
                   (german_word, turkish_meaning, example_sentence_de, article_id, category_id, type_id, level)
                   VALUES ('Hund', 'köpek', 'Der Hund ist groß.', 1, 1, 1, 'A1')'''
            )
            conn.execute(
                '''INSERT INTO words
                   (german_word, turkish_meaning, example_sentence_de, article_id, category_id, type_id, level)
                   VALUES ('kaufen', 'satın almak', 'Ich kaufe Brot.', 1, 1, 1, 'A2')'''
            )
    with app.test_client() as client:
        yield client
    os.close(db_fd)
    os.unlink(db_path)

# GET /words/?level=A1 -- sadece A1 kelimeleri
def test_get_words_filter_by_level_A1(level_seeded_client):
    response = level_seeded_client.get('/words/?level=A1')
    assert response.status_code == 200
    data = response.get_json()
    assert len(data) == 1
    assert data[0]['german_word'] == 'Hund'

# GET /words/?level=a1 -- upper() kontrolü, küçük harf de çalışır
def test_get_words_filter_by_level_lowercase(level_seeded_client):
    response = level_seeded_client.get('/words/?level=a1')
    assert response.status_code == 200
    data = response.get_json()
    assert len(data) == 1
    assert data[0]['german_word'] == 'Hund'