import os
from flask import Flask
from flask_jwt_extended import JWTManager
from .database import init_db
from .routes import register_blueprints


def create_app(test_config=None):
    app = Flask(__name__, instance_relative_config=True)
    app.config.from_mapping(
        SECRET_KEY=os.environ.get('SECRET_KEY', 'dev'),
        JWT_SECRET_KEY=os.environ.get('JWT_SECRET_KEY', 'dev-secret'),
    )

    if test_config:
        app.config.update(test_config)

    JWTManager(app)

    with app.app_context():
        init_db()
    register_blueprints(app)

    return app