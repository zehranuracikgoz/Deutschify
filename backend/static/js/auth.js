'use strict';

const API_URL = window.location.hostname === 'localhost'
  ? 'http://localhost:5000'
  : window.location.origin;

let _toastTimer;
function toast(msg) {
    const el = document.getElementById('toast');
    el.textContent = msg;
    el.classList.add('show');
    clearTimeout(_toastTimer);
    _toastTimer = setTimeout(() => el.classList.remove('show'), 2800);
}

const getToken  = () => localStorage.getItem('access_token');
const getUserId = () => localStorage.getItem('user_id');
const authHdr   = () => ({
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + getToken()
});

function requireAuth() {
    if (!getToken()) {
        window.location.href = '/web/login';
        return false;
    }
    return true;
}

async function doLogin() {
    const email = document.getElementById('li-email').value.trim();
    const pass  = document.getElementById('li-pass').value;
    const errEl = document.getElementById('li-err');
    errEl.textContent = '';

    if (!email || !pass) { errEl.textContent = 'E-posta ve şifre zorunlu.'; return; }

    try {
        const res  = await fetch(API_URL + '/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password: pass })
        });
        const data = await res.json();
        if (!res.ok) { errEl.textContent = data.error || 'Giriş başarısız.'; return; }

        localStorage.setItem('access_token', data.access_token);
        localStorage.setItem('user_id',      data.user_id);
        localStorage.setItem('username',     data.username);
        window.location.href = '/web/';
    } catch { errEl.textContent = 'Bağlantı hatası.'; }
}

async function doRegister() {
    const uname = document.getElementById('re-uname').value.trim();
    const email = document.getElementById('re-email').value.trim();
    const pass  = document.getElementById('re-pass').value;
    const errEl = document.getElementById('re-err');
    errEl.textContent = '';

    if (!uname || !email || !pass) { errEl.textContent = 'Tüm alanlar zorunlu.'; return; }

    try {
        const res  = await fetch(API_URL + '/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: uname, email, password: pass })
        });
        const data = await res.json();
        if (!res.ok) { errEl.textContent = data.error || 'Kayıt başarısız.'; return; }

        toast('Kayıt başarılı! Giriş yapılıyor…');
        const loginRes  = await fetch(API_URL + '/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password: pass })
        });
        const loginData = await loginRes.json();
        if (loginRes.ok) {
            localStorage.setItem('access_token', loginData.access_token);
            localStorage.setItem('user_id',      loginData.user_id);
            localStorage.setItem('username',     loginData.username);
            window.location.href = '/web/';
        } else {
            window.location.href = '/web/login';
        }
    } catch { errEl.textContent = 'Bağlantı hatası.'; }
}

function doLogout() {
    localStorage.clear();
    window.location.href = '/web/login';
}