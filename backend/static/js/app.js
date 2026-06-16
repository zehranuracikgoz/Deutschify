'use strict';

const DAYS = ['Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt', 'Paz'];

function formatDuration(totalMinutes) {
    const minutes = Math.round(totalMinutes || 0);
    const hours = Math.floor(minutes / 60);
    const mins  = minutes % 60;
    return hours > 0 ? `${hours}sa ${mins}dk` : `${mins}dk`;
}

async function loadHome() {
    document.getElementById('greet-name').textContent = localStorage.getItem('username') || '—';

    document.getElementById('h-wde').textContent  = '…';
    document.getElementById('h-wtr').textContent  = 'yükleniyor';
    document.getElementById('h-exde').textContent = '—';
    document.getElementById('h-extr').textContent = '';

    try {
        const res  = await fetch(`${API_URL}/study/queue/${getUserId()}?limit=1`);
        const data = await res.json();
        const q    = data.queue;
        if (q && q.length > 0) {
            const w = q[0];
            document.getElementById('h-wde').textContent  = w.german_word         || '—';
            document.getElementById('h-wtr').textContent  = w.turkish_meaning     || '';
            document.getElementById('h-exde').textContent = w.example_sentence_de || '';
            document.getElementById('h-extr').textContent = '';
        } else {
            document.getElementById('h-wde').innerHTML    = '<svg class="complete-icon" viewBox="0 0 24 24"><path d="M12,2C6.47,2 2,6.47 2,12s4.47,10 10,10 10,-4.47 10,-10S17.53,2 12,2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8z"/></svg>';
            document.getElementById('h-wtr').textContent  = 'Bugünkü kelimeleri tamamladın!';
            document.getElementById('h-exde').textContent = '';
        }
    } catch {
        document.getElementById('h-wde').textContent = '—';
        document.getElementById('h-wtr').textContent = 'Yüklenemedi';
    }

    try {
        const statsRes = await fetch(API_URL + '/study/stats', { headers: authHdr() });
        if (statsRes.ok) {
            const stats = await statsRes.json();
            const weeklyMinutes = stats.weekly_minutes || new Array(7).fill(0);
            renderChart(weeklyMinutes);
            document.getElementById('h-chart-time').textContent = formatDuration(weeklyMinutes.reduce((a, b) => a + b, 0));
            document.getElementById('greet-xp').textContent     = (stats.total_xp     ?? 0) + ' XP';
            document.getElementById('greet-streak').textContent = (stats.daily_streak ?? 0) + ' gün';
            document.getElementById('h-correct').textContent    = stats.total_correct ?? 0;
            document.getElementById('h-rate').textContent       = (stats.success_rate ?? 0) + '%';
        }
    } catch { /* haftalık grafik olmadan da sayfa kullanılabilir */ }
}

async function loadDashboard() {
    ['d-xp', 'd-streak', 'd-correct', 'd-rate'].forEach(id =>
        document.getElementById(id).textContent = '—'
    );

    try {
        const res = await fetch(API_URL + '/study/stats', { headers: authHdr() });
        if (!res.ok) { toast('İstatistikler yüklenemedi.'); return; }
        const d   = await res.json();

        document.getElementById('d-xp').textContent      = (d.total_xp    ?? 0) + ' XP';
        document.getElementById('d-streak').textContent  = (d.daily_streak ?? 0) + ' gün';
        document.getElementById('d-correct').textContent = d.total_correct  ?? 0;
        document.getElementById('d-rate').textContent    = (d.success_rate  ?? 0) + '%';

        const weeklyMinutes = d.weekly_minutes || new Array(7).fill(0);
        renderChart(weeklyMinutes);
        document.getElementById('d-chart-time').textContent = formatDuration(weeklyMinutes.reduce((a, b) => a + b, 0));
    } catch { toast('Bağlantı hatası.'); }
}

function renderChart(sessions) {
    const chart = document.getElementById('bar-chart');
    chart.innerHTML = '';
    const max = Math.max(...sessions, 1);

    sessions.forEach((count, i) => {
        const pct = Math.round((count / max) * 100);
        const col = document.createElement('div');
        col.className = 'bar-col';
        col.innerHTML = `
            <div class="bar-wrap">
                <div class="bar-track"></div>
                <div class="bar-fill" style="height:${pct}%;${count === 0 ? 'background:rgba(183,148,214,0.3)' : ''}"></div>
            </div>
            <div class="bar-day">${DAYS[i]}</div>
        `;
        chart.appendChild(col);
    });
}

async function loadHistory() {
    const container = document.getElementById('history-list');
    const emptyEl   = document.getElementById('history-empty');
    container.innerHTML = '';
    emptyEl.style.display = 'none';

    try {
        const res = await fetch(API_URL + '/study/history', { headers: authHdr() });
        if (!res.ok) { toast('Geçmiş yüklenemedi.'); return; }
        const data     = await res.json();
        const sessions = data.sessions || [];

        if (sessions.length === 0) {
            emptyEl.style.display = 'block';
            return;
        }

        sessions.forEach(s => {
            const card = document.createElement('div');
            card.className = 'session-card';

            const dateEl = document.createElement('div');
            dateEl.className = 'session-date';
            dateEl.textContent = s.date;

            const row = document.createElement('div');
            row.className = 'session-row';

            const correctEl = document.createElement('span');
            correctEl.className = 'session-correct';
            correctEl.textContent = `Doğru: ${s.correct}`;

            const wrongEl = document.createElement('span');
            wrongEl.className = 'session-wrong';
            wrongEl.textContent = `Yanlış: ${s.wrong}`;

            row.appendChild(correctEl);
            row.appendChild(wrongEl);
            card.appendChild(dateEl);
            card.appendChild(row);
            container.appendChild(card);
        });
    } catch { toast('Bağlantı hatası.'); }
}

async function loadProfile() {
    document.getElementById('pr-name').textContent = localStorage.getItem('username') || '—';

    try {
        const res = await fetch(API_URL + '/auth/me', { headers: authHdr() });
        if (!res.ok) { toast('Profil yüklenemedi.'); return; }
        const d   = await res.json();

        document.getElementById('pr-name').textContent   = d.username     || '—';
        document.getElementById('pr-email').textContent  = d.email        || '—';
        document.getElementById('pr-level').textContent  = d.level        || 'A1';
        document.getElementById('pr-xp').textContent     = (d.total_xp    ?? 0) + ' XP';
        document.getElementById('pr-streak').textContent = (d.daily_streak ?? 0) + ' gün';
    } catch { toast('Bağlantı hatası.'); }
}