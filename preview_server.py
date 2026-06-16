"""Sadece web template'lerini önizlemek için"""
import os
from flask import Flask
from backend.routes.web_routes import web_bp

app = Flask(
    __name__,
    template_folder=os.path.join('backend', 'templates'),
    static_folder=os.path.join('backend', 'static'),
)
app.register_blueprint(web_bp)

if __name__ == '__main__':
    app.run(port=8080, debug=True)