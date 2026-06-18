import os
from datetime import timedelta
from flask import Flask
from flask_cors import CORS
from flask_jwt_extended import JWTManager
from .database import init_db
from .routes import register_blueprints


def create_app(test_config=None):
    app = Flask(__name__,
                static_folder=os.path.join(os.path.dirname(__file__), 'static'),
                static_url_path='/static',
                instance_relative_config=True)
    import logging
    logging.warning(f"Static folder: {app.static_folder}")
    CORS(app)

    secret_key = os.environ.get('SECRET_KEY')
    jwt_secret = os.environ.get('JWT_SECRET_KEY')
    if not secret_key or not jwt_secret:
        raise RuntimeError('SECRET_KEY ve JWT_SECRET_KEY ortam değişkenleri zorunludur')

    app.config.from_mapping(
        SECRET_KEY=secret_key,
        JWT_SECRET_KEY=jwt_secret,
        JWT_ACCESS_TOKEN_EXPIRES=timedelta(hours=24),
    )

    if test_config:
        app.config.update(test_config)

    JWTManager(app)

    with app.app_context():
        init_db()
    register_blueprints(app)

    return app