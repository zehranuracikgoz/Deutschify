import os
from flask import Flask
from .database import init_db
from .routes import register_blueprints


def create_app():
    app = Flask(__name__, instance_relative_config=True)
    app.config.from_mapping(
        SECRET_KEY=os.environ.get('SECRET_KEY', 'dev'),
    )

    init_db()
    register_blueprints(app)

    return app