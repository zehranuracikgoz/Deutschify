import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

from backend import create_app

# create_app() fonksiyonunun doğrulanması
def test_create_app_returns_app():
    app = create_app()
    assert app is not None

# SECRET_KEY'in none olmamasınının doğrulanması
def test_secret_key_is_set():
    app = create_app()
    assert app.config['SECRET_KEY'] is not None

# /health endpoint'inin 200 döndürmesinin doğrulanması - blueprint kayıt adımının başarılı olması
def test_app_has_health_route():
    app = create_app()
    client = app.test_client()
    response = client.get('/health')
    assert response.status_code == 200