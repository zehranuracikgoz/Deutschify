from flask import Blueprint, render_template

web_bp = Blueprint('web', __name__, url_prefix='/web')


@web_bp.route('/')
def home():
    return render_template('home.html')


@web_bp.route('/login')
def login():
    return render_template('login.html')


@web_bp.route('/register')
def register():
    return render_template('register.html')


@web_bp.route('/dashboard')
def dashboard():
    return render_template('dashboard.html')


@web_bp.route('/profile')
def profile():
    return render_template('profile.html')


@web_bp.route('/history')
def history():
    return render_template('history.html')