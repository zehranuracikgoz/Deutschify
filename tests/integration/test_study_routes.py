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
            cursor.execute("DELETE FROM ai_feedback_logs")
            cursor.execute("DELETE FROM study_sessions")
            cursor.execute("DELETE FROM user_progress")
            cursor.execute("DELETE FROM words")
            cursor.execute("DELETE FROM users")
            cursor.execute("DELETE FROM word_categories")
            cursor.execute("DELETE FROM word_types")
            cursor.execute("DELETE FROM articles")

@pytest.fixture
def seeded_client():
    """Kullanıcı ve kelime eklenmiş test client'ı."""
    app = create_app({'TESTING': True})
    with app.test_client() as client:
        client.post('/auth/register', json={
            'username': 'studyuser',
            'email': 'study@test.com',
            'password': 'Test123!'
        })
        login_res = client.post('/auth/login', json={'email': 'study@test.com', 'password': 'Test123!'})
        user_id = login_res.get_json()['user_id']
        token = login_res.get_json()['access_token']
        with app.app_context():
            with get_db() as conn:
                cursor = conn.cursor()
                cursor.execute(
                    "INSERT INTO articles (name) VALUES ('der') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id"
                )
                article_id = cursor.fetchone()[0]
            with get_db() as conn:
                cursor = conn.cursor()
                cursor.execute(
                    "INSERT INTO word_categories (name) VALUES ('Temel') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id"
                )
                category_id = cursor.fetchone()[0]
            with get_db() as conn:
                cursor = conn.cursor()
                cursor.execute(
                    "INSERT INTO word_types (name) VALUES ('Noun') ON CONFLICT (name) DO UPDATE SET name=EXCLUDED.name RETURNING id"
                )
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
        yield (client, user_id, word_id, token)
    with app.app_context():
        with get_db() as conn:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM ai_feedback_logs")
            cursor.execute("DELETE FROM study_sessions")
            cursor.execute("DELETE FROM user_progress")
            cursor.execute("DELETE FROM words")
            cursor.execute("DELETE FROM users")
            cursor.execute("DELETE FROM word_categories")
            cursor.execute("DELETE FROM word_types")
            cursor.execute("DELETE FROM articles")


def auth_header(token):
    return {'Authorization': f'Bearer {token}'}


# kullanıcının hiç progressi yoksa yeni kelimeler new olarak gözükecek
def test_queue_returns_new_words(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.get(f'/study/queue/{user_id}', headers=auth_header(token))
    assert response.status_code == 200
    data = response.get_json()
    assert 'queue' in data
    assert len(data['queue']) == 1
    assert data['queue'][0]['status'] == 'new'
    assert data['queue'][0]['german_word'] in ['Hund', 'kaufen']

# limit parametresi çalışıyor mu
def test_queue_limit_parameter(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.get(f'/study/queue/{user_id}?limit=1', headers=auth_header(token))
    assert response.status_code == 200
    data = response.get_json()
    assert len(data['queue']) <= 1


# geçersiz limit=400
def test_queue_invalid_limit(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.get(f'/study/queue/{user_id}?limit=0', headers=auth_header(token))
    assert response.status_code == 400

    response = client.get(f'/study/queue/{user_id}?limit=abc', headers=auth_header(token))
    assert response.status_code == 400

# hiç kelime yoksa boş kuyruk ve mesaj döner
def test_queue_empty_when_no_words(client):
    client.post('/auth/register', json={
        'username': 'emptyuser',
        'email': 'empty@test.com',
        'password': 'Test123!'
    })
    login_res = client.post('/auth/login', json={'email': 'empty@test.com', 'password': 'Test123!'})
    token = login_res.get_json()['access_token']
    user_id = login_res.get_json()['user_id']
    response = client.get(f'/study/queue/{user_id}', headers=auth_header(token))
    assert response.status_code == 200
    data = response.get_json()
    assert data['queue'] == []
    assert 'message' in data

# geçerli cevap kaydedilince 200 ve xp dönecek
def test_submit_answer_quality_5_gives_xp(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.post('/study/answer', json={
        'user_id': user_id,
        'word_id': word_id,
        'quality': 5
    }, headers=auth_header(token))
    assert response.status_code == 200
    data = response.get_json()
    assert data['message'] == 'ok'
    assert data['xp_earned'] == 10
    assert data['status'] in ('learning', 'review')

# quality < 3 ise xp kazanılmaz
def test_submit_answer_quality_2_no_xp(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.post('/study/answer', json={
        'user_id': user_id,
        'word_id': word_id,
        'quality': 2
    }, headers=auth_header(token))
    assert response.status_code == 200
    assert response.get_json()['xp_earned'] == 0

# quality aralık dışıysa 400
def test_submit_answer_invalid_quality(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.post('/study/answer', json={
        'user_id': user_id,
        'word_id': word_id,
        'quality': 9
    }, headers=auth_header(token))
    assert response.status_code == 400

# quality 0 geçersiz (min 1)
def test_submit_answer_quality_0_invalid(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.post('/study/answer', json={
        'user_id': user_id,
        'word_id': word_id,
        'quality': 0
    }, headers=auth_header(token))
    assert response.status_code == 400

# eksik alan varsa 400
def test_submit_answer_missing_fields(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.post('/study/answer', json={
        'user_id': user_id
    }, headers=auth_header(token))
    assert response.status_code == 400


def test_start_session_returns_201_and_session_id(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.post('/study/session/start', json={'user_id': user_id}, headers=auth_header(token))
    assert response.status_code == 201
    data = response.get_json()
    assert 'session_id' in data


def test_end_session_returns_200(seeded_client):
    client, user_id, word_id, token = seeded_client
    start = client.post('/study/session/start', json={'user_id': user_id}, headers=auth_header(token))
    session_id = start.get_json()['session_id']
    response = client.put(f'/study/session/{session_id}/end', headers=auth_header(token))
    assert response.status_code == 200


# token olmadan /study/stats = 401
def test_stats_requires_auth(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.get('/study/stats')
    assert response.status_code == 401


# bitmiş oturumun süresi weekly_minutes'a dakika olarak yansıması için
def test_stats_returns_weekly_minutes(seeded_client):
    client, user_id, word_id, token = seeded_client

    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """INSERT INTO study_sessions (user_id, session_start, session_end, correct_answers, wrong_answers)
               VALUES (%s, NOW() - INTERVAL '30 minutes', NOW(), 0, 0)""",
            (user_id,)
        )

    response = client.get('/study/stats', headers=auth_header(token))
    assert response.status_code == 200
    data = response.get_json()
    assert 'weekly_minutes' in data
    assert len(data['weekly_minutes']) == 7
    assert data['weekly_minutes'][-1] == 30


# session_end NULL olan (bitmemiş) oturumlar 5 dakika varsayılan ile sayılmalı
def test_stats_counts_unfinished_sessions_as_default_minutes(seeded_client):
    client, user_id, word_id, token = seeded_client

    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute(
            """INSERT INTO study_sessions (user_id, session_start, session_end, correct_answers, wrong_answers)
               VALUES (%s, NOW(), NULL, 0, 0)""",
            (user_id,)
        )

    response = client.get('/study/stats', headers=auth_header(token))
    assert response.status_code == 200
    data = response.get_json()
    assert data['weekly_minutes'][-1] == 5

# quality=4 gönderilince user_progress'te next_review_date güncellenmeli
def test_answer_quality4_updates_next_review_date(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.post('/study/answer', json={
        'user_id': user_id,
        'word_id': word_id,
        'quality': 4
    }, headers=auth_header(token))
    assert response.status_code == 200
    data = response.get_json()
    assert data['message'] == 'ok'
    assert data['xp_earned'] == 10

    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute(
            'SELECT next_review_date, status FROM user_progress WHERE user_id = %s AND word_id = %s',
            (user_id, word_id)
        )
        row = cursor.fetchone()

    assert row is not None, "user_progress kaydı oluşturulmamış"
    assert row[0] is not None, "next_review_date NULL olmamalı"
    assert row[1] in ('learning', 'review'), f"Beklenmeyen status: {row[1]}"


# daha önce cevaplanan kelime due_rows olarak kuyruğa girmeli
def test_queue_includes_due_words_after_answer(seeded_client):
    client, user_id, word_id, token = seeded_client
    client.post('/study/answer', json={
        'user_id': user_id,
        'word_id': word_id,
        'quality': 1
    }, headers=auth_header(token))
    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute(
            "UPDATE user_progress SET next_review_date = CURRENT_DATE - 1 WHERE user_id = %s AND word_id = %s",
            (user_id, word_id)
        )
    response = client.get(f'/study/queue/{user_id}', headers=auth_header(token))
    assert response.status_code == 200
    data = response.get_json()
    assert 'queue' in data
    assert len(data['queue']) > 0
    assert any(w['word_id'] == word_id for w in data['queue'])


# session/start DB'ye gerçekten kayıt yazmalı
def test_session_start_inserts_db_record(seeded_client):
    client, user_id, word_id, token = seeded_client
    response = client.post('/study/session/start', json={'user_id': user_id}, headers=auth_header(token))
    assert response.status_code == 201
    session_id = response.get_json()['session_id']

    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute(
            'SELECT user_id, session_end FROM study_sessions WHERE id = %s',
            (session_id,)
        )
        row = cursor.fetchone()

    assert row is not None, "study_sessions kaydı oluşturulmamış"
    assert row[0] == user_id, "user_id eşleşmiyor"
    assert row[1] is None, "Yeni oturumun session_end'i NULL olmalı"

# IDOR negatif testler
def _register_and_login(client, username, email):
    client.post('/auth/register', json={
        'username': username,
        'email': email,
        'password': 'Test123!'
    })
    res = client.post('/auth/login', json={'email': email, 'password': 'Test123!'})
    data = res.get_json()
    return data['user_id'], data['access_token']


def test_queue_rejects_mismatched_user_id(client):
    user_a_id, token_a = _register_and_login(client, 'idor_user_a1', 'idor_a1@test.com')
    user_b_id, token_b = _register_and_login(client, 'idor_user_b1', 'idor_b1@test.com')
    response = client.get(f'/study/queue/{user_b_id}', headers=auth_header(token_a))
    assert response.status_code == 403


def test_answer_rejects_mismatched_user_id(client):
    user_a_id, token_a = _register_and_login(client, 'idor_user_a2', 'idor_a2@test.com')
    user_b_id, token_b = _register_and_login(client, 'idor_user_b2', 'idor_b2@test.com')
    response = client.post('/study/answer', json={
        'user_id': user_b_id,
        'word_id': 1,
        'quality': 5
    }, headers=auth_header(token_a))
    assert response.status_code == 403


def test_session_start_rejects_mismatched_user_id(client):
    user_a_id, token_a = _register_and_login(client, 'idor_user_a3', 'idor_a3@test.com')
    user_b_id, token_b = _register_and_login(client, 'idor_user_b3', 'idor_b3@test.com')
    response = client.post('/study/session/start', json={
        'user_id': user_b_id
    }, headers=auth_header(token_a))
    assert response.status_code == 403


def test_end_session_rejects_other_users_session(client):
    user_a_id, token_a = _register_and_login(client, 'idor_user_a4', 'idor_a4@test.com')
    user_b_id, token_b = _register_and_login(client, 'idor_user_b4', 'idor_b4@test.com')
    start_res = client.post('/study/session/start', json={
        'user_id': user_b_id
    }, headers=auth_header(token_b))
    assert start_res.status_code ==201
    session_id = start_res.get_json()['session_id']
    response = client.put(f'/study/session/{session_id}/end', headers=auth_header(token_a))
    assert response.status_code == 403