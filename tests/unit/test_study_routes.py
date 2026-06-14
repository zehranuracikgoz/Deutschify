import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

import pytest
import psycopg2.extras
from backend import create_app
from backend.database import get_db


@pytest.fixture
def client():
    app = create_app({'TESTING': True})
    yield app.test_client()
    with app.app_context():
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("TRUNCATE TABLE user_progress, study_sessions, words, users, articles, word_categories, word_types CASCADE")

@pytest.fixture
def seeded_client():
    """Kullanıcı ve kelime eklenmiş test client'ı."""
    app = create_app({'TESTING': True})
    with app.test_client() as client:
        # kullanıcı oluştur
        client.post('/auth/register', json={
            'username': 'studyuser',
            'email': 'study@test.com',
            'password': 'Test123!'
        })
        login_res = client.post('/auth/login', json={'email': 'study@test.com', 'password': 'Test123!'})
        user_id = login_res.get_json()['user_id']
        # kelime ekle -doğrudan DB
        with app.app_context():
            with get_db() as conn:
                cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
                cursor.execute(
                    "INSERT INTO articles (name) VALUES ('der') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id"
                )
                article_id = cursor.fetchone()['id']
            with get_db() as conn:
                cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
                cursor.execute(
                    "INSERT INTO word_categories (name) VALUES ('Temel') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id"
                )
                category_id = cursor.fetchone()['id']
            with get_db() as conn:
                cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
                cursor.execute(
                    "INSERT INTO word_types (name) VALUES ('Noun') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id"
                )
                type_id = cursor.fetchone()['id']
            with get_db() as conn:
                cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
                cursor.execute(
                    '''INSERT INTO words
                       (german_word, turkish_meaning, example_sentence_de, article_id, category_id, type_id)
                       VALUES ('Hund', 'köpek', 'Der Hund ist groß.', %s, %s, %s) ON CONFLICT (german_word) DO UPDATE SET german_word=EXCLUDED.german_word RETURNING id''',
                    (article_id, category_id, type_id)
                )
                word_id = cursor.fetchone()['id']
        yield (client, user_id, word_id)
    with app.app_context():
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("TRUNCATE TABLE user_progress, study_sessions, words, users, articles, word_categories, word_types CASCADE")

# kullanıcının hiç progressi yoksa yeni kelimeler new olarak gözükecek
def test_queue_returns_new_words(seeded_client):
    client, user_id, word_id = seeded_client
    response = client.get(f'/study/queue/{user_id}')
    assert response.status_code == 200
    data = response.get_json()
    assert 'queue' in data
    assert len(data['queue']) == 1
    assert data['queue'][0]['status'] == 'new'
    assert data['queue'][0]['german_word'] in ['Hund', 'kaufen']

# limit parametresi çalışıyor mu
def test_queue_limit_parameter(seeded_client):
    client, user_id, word_id = seeded_client
    response = client.get(f'/study/queue/{user_id}?limit=1')
    assert response.status_code == 200
    data = response.get_json()
    assert len(data['queue']) <= 1


# geçersiz limit=400
def test_queue_invalid_limit(seeded_client):
    client, user_id, word_id = seeded_client
    response = client.get(f'/study/queue/{user_id}?limit=0')
    assert response.status_code == 400

    response = client.get(f'/study/queue/{user_id}?limit=abc')
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
    client, user_id, word_id = seeded_client
    response = client.post('/study/answer', json={
        'user_id': user_id,
        'word_id': word_id,
        'quality': 5
    })
    assert response.status_code == 200
    data = response.get_json()
    assert data['message'] == 'ok'
    assert data['xp_earned'] == 10
    assert data['status'] in ('learning', 'review')

# quality < 3 ise xp kazanılmaz
def test_submit_answer_quality_2_no_xp(seeded_client):
    client, user_id, word_id = seeded_client
    response = client.post('/study/answer', json={
        'user_id': user_id,
        'word_id': word_id,
        'quality': 2
    })
    assert response.status_code == 200
    assert response.get_json()['xp_earned'] == 0

# quality aralık dışıysa 400
def test_submit_answer_invalid_quality(seeded_client):
    client, user_id, word_id = seeded_client
    response = client.post('/study/answer', json={
        'user_id': user_id,
        'word_id': word_id,
        'quality': 9
    })
    assert response.status_code == 400

# quality 0 geçersiz (min 1)
def test_submit_answer_quality_0_invalid(seeded_client):
    client, user_id, word_id = seeded_client
    response = client.post('/study/answer', json={
        'user_id': user_id,
        'word_id': word_id,
        'quality': 0
    })
    assert response.status_code == 400

# eksik alan varsa 400
def test_submit_answer_missing_fields(seeded_client):
    client, user_id, word_id = seeded_client
    response = client.post('/study/answer', json={
        'user_id': user_id
    })
    assert response.status_code == 400


def test_start_session_returns_201_and_session_id(seeded_client):
    client, user_id, word_id = seeded_client
    response = client.post('/study/session/start', json={'user_id': user_id})
    assert response.status_code == 201
    data = response.get_json()
    assert 'session_id' in data


def test_end_session_returns_200(seeded_client):
    client, user_id, word_id = seeded_client
    start = client.post('/study/session/start', json={'user_id': user_id})
    session_id = start.get_json()['session_id']
    response = client.put(f'/study/session/{session_id}/end')
    assert response.status_code == 200