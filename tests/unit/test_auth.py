import sys
import os
import tempfile

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

import pytest
from backend import create_app

# auth endpointleri için http kodları için
# register ve login akışlarındaki edge case'ler
@pytest.fixture
def client():
    db_fd, db_path = tempfile.mkstemp()
    app = create_app({'TESTING': True, 'DATABASE': db_path})
    yield app.test_client()
    os.close(db_fd)
    os.unlink(db_path)

# geçerli bilgilerde = 201
def test_register_success(client):
    response = client.post('/auth/register', json={
        'username': 'testuser',
        'email': 'testuser@test.com',
        'password': 'Test123!'
    })
    assert response.status_code == 201

# aynı email ile tekrar kayıtta = 409
def test_register_duplicate_email(client):
    payload = {
        'username': 'testuser',
        'email': 'duplicate@test.com',
        'password': 'Test123!'
    }
    client.post('/auth/register', json=payload)
    response = client.post('/auth/register', json=payload)
    assert response.status_code == 409

# doğru bilgilerle girişte = 200
def test_login_success(client):
    client.post('/auth/register', json={
        'username': 'loginuser',
        'email': 'login@test.com',
        'password': 'Test123!'
    })
    response = client.post('/auth/login', json={
        'email': 'login@test.com',
        'password': 'Test123!'
    })
    assert response.status_code == 200

# yanlış şifrede = 401
def test_login_wrong_password(client):
    client.post('/auth/register', json={
        'username': 'wrongpass',
        'email': 'wrongpass@test.com',
        'password': 'Test123!'
    })
    response = client.post('/auth/login', json={
        'email': 'wrongpass@test.com',
        'password': 'WrongPassword!'
    })
    assert response.status_code == 401

# eksik alan olunca = 400
def test_login_missing_fields(client):
    response = client.post('/auth/login', json={
        'email': 'missing@test.com'
    })
    assert response.status_code == 400