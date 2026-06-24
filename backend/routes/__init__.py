from flask import Blueprint, jsonify, redirect, url_for
from .auth_routes import auth_bp
from .word_routes import word_bp
from .study_routes import study_bp
from .tts_routes import tts_bp
from .web_routes import web_bp
from .grammar_routes import grammar_bp

main_bp = Blueprint('main', __name__)


@main_bp.route('/')
def index():
    return redirect(url_for('web.home'))


@main_bp.route('/health')
def health():
    return jsonify({'status': 'ok'})


def register_blueprints(app):
    app.register_blueprint(main_bp)
    app.register_blueprint(auth_bp)
    app.register_blueprint(word_bp)
    app.register_blueprint(study_bp)
    app.register_blueprint(tts_bp)
    app.register_blueprint(web_bp)
    app.register_blueprint(grammar_bp)