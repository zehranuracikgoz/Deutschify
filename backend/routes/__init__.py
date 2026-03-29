from flask import Blueprint, jsonify
from .auth_routes import auth_bp
from .word_routes import word_bp
from .study_routes import study_bp

main_bp = Blueprint('main', __name__)

@main_bp.route('/health')
def health():
    return jsonify({'status': 'ok'})

def register_blueprints(app):
    app.register_blueprint(main_bp)
    app.register_blueprint(auth_bp)
    app.register_blueprint(word_bp)
    app.register_blueprint(study_bp)