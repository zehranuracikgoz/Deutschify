import sys
import os
import tempfile

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

import pytest
from backend import create_app


@pytest.fixture
def client():
    db_fd, db_path = tempfile.mkstemp()
    app = create_app({'TESTING': True, 'DATABASE': db_path})
    yield app.test_client()
    os.close(db_fd)
    os.unlink(db_path)

@pytest.fixture
def seeded_client():
    """Kullanıcı ve kelime eklenmiş test client'ı."""
    db_fd, db_path = tempfile.mkstemp()
    app = create_app({'TESTING': True, 'DATABASE': db_path})
    with app.test_client() as client:
        # kullanıcı oluştur
        client.post('/auth/register', json={
            'username': 'studyuser',
            'email': 'study@test.com',
            'password': 'Test123!'
        })
        # kelime ekle -doğrudan DB
        from backend.database import get_db
        with app.app_context():
            with get_db() as conn:
                conn.execute(
                    "INSERT INTO articles (name) VALUES ('der')"
                )
                conn.execute(
                    "INSERT INTO word_categories (name) VALUES ('Temel')"
                )
                conn.execute(
                    "INSERT INTO word_types (name) VALUES ('Noun')"
                )
                conn.execute(
                    '''INSERT INTO words
                       (german_word, turkish_meaning, example_sentence_de, article_id, category_id, type_id)
                       VALUES ('Hund', 'köpek', 'Der Hund ist groß.', 1, 1, 1)'''
                )
        yield client
    os.close(db_fd)
    os.unlink(db_path)

# kullanıcının hiç progressi yoksa yeni kelimeler new olarak gözükecek
def test_queue_returns_new_words(seeded_client):
    response = seeded_client.get('/study/queue/1')
    assert response.status_code == 200
    data = response.get_json()
    assert 'queue' in data
    assert len(data['queue']) == 1
    assert data['queue'][0]['status'] == 'new'
    assert data['queue'][0]['german_word'] == 'Hund'

# limit parametresi çalışıyor mu
def test_queue_limit_parameter(seeded_client):
    response = seeded_client.get('/study/queue/1?limit=1')
    assert response.status_code == 200
    data = response.get_json()
    assert len(data['queue']) <= 1


# geçersiz limit=400
def test_queue_invalid_limit(seeded_client):
    response = seeded_client.get('/study/queue/1?limit=0')
    assert response.status_code == 400

    response = seeded_client.get('/study/queue/1?limit=abc')
    assert response.status_code == 400

# hiç kelime yoksa boş kuyruk ve mesaj döner
def test_queue_empty_when_no_words(client):
    client.post('/auth/register', json={
        'username': 'emptyuser',
        'email': 'empty@test.com',
        'password': 'Test123!'
    })
    response = client.get('/study/queue/1')
    assert response.status_code == 200
    data = response.get_json()
    assert data['queue'] == []
    assert 'message' in data

# geçerli cevap kaydedilince 200 ve xp dönecek
def test_submit_answer_quality_5_gives_xp(seeded_client):
    response = seeded_client.post('/study/answer', json={
        'user_id': 1,
        'word_id': 1,
        'quality': 5
    })
    assert response.status_code == 200
    data = response.get_json()
    assert data['message'] == 'ok'
    assert data['xp_earned'] == 10
    assert data['status'] in ('learning', 'review')

# quality < 3 ise xp kazanılmaz
def test_submit_answer_quality_2_no_xp(seeded_client):
    response = seeded_client.post('/study/answer', json={
        'user_id': 1,
        'word_id': 1,
        'quality': 2
    })
    assert response.status_code == 200
    assert response.get_json()['xp_earned'] == 0

# quality aralık dışıysa 400
def test_submit_answer_invalid_quality(seeded_client):
    response = seeded_client.post('/study/answer', json={
        'user_id': 1,
        'word_id': 1,
        'quality': 9
    })
    assert response.status_code == 400

# quality 0 geçersiz (min 1)
def test_submit_answer_quality_0_invalid(seeded_client):
    response = seeded_client.post('/study/answer', json={
        'user_id': 1,
        'word_id': 1,
        'quality': 0
    })
    assert response.status_code == 400

# eksik alan varsa 400
def test_submit_answer_missing_fields(seeded_client):
    response = seeded_client.post('/study/answer', json={
        'user_id': 1
    })
    assert response.status_code == 400