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
            document.getElementById('h-correct').textContent = stats.max_streak ?? 0;
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
        document.getElementById('d-correct').textContent = d.max_streak  ?? 0;
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

        const moduleLabels = { flashcard: 'Flashcard', review: 'Tekrar', artikel: 'Artikel', grammar: 'Dil Bilgisi' };

        const groups = {};
        sessions.forEach(s => {
            if (!groups[s.date]) groups[s.date] = [];
            groups[s.date].push(s);
        });

        Object.keys(groups).sort((a, b) =>b.localeCompare(a)).forEach(date => {
            const dayHeader = document.createElement('div');
            dayHeader.className = 'history-day-header';
            dayHeader.textContent = formatHistoryDate(date);
            container.appendChild(dayHeader);

            groups[date].forEach(s => {
                const rate = s.success_rate;
                const hasData = s.correct + s.wrong > 0;
                const showRate = s.module_type !== 'flashcard';
                const rateHtml = showRate && hasData
                    ? `<div class="session-rate-bar"><div class="session-rate-fill" style="width:${rate ?? 0}%"></div></div>
                       <div class="session-rate-label">${rate ?? 0}% başarı &mdash; ${s.correct} doğru, ${s.wrong} yanlış</div>`
                    : '';

                const card = document.createElement('div');
                card.className = 'session-card';
                card.innerHTML = `
                    <div class="session-header">
                        <span class="session-module-badge ${s.module_type}">${moduleLabels[s.module_type] || s.module_type}</span>
                        <span class="session-meta">${s.time}</span>
                        <span class="session-xp">+${s.xp_earned} XP</span>
                    </div>
                    ${rateHtml}
                `;
                container.appendChild(card);
            });
        });
    } catch { toast('Bağlantı hatası.'); }
}

function formatHistoryDate(dateStr) {
    const today     = new Date().toISOString().slice(0, 10);
    const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10);
    if (dateStr === today)     return 'Bugün';
    if (dateStr === yesterday) return 'Dün';
    const d = new Date(dateStr + 'T00:00:00');
    const months = ['Ocak','Şubat','Mart','Nisan','Mayıs','Haziran','Temmuz','Ağustos','Eylül','Ekim','Kasım','Aralık'];
    return `${d.getDate()} ${months[d.getMonth()]}`;
}

async function loadProfile() {
    document.getElementById('pr-name').textContent = localStorage.getItem('username') || '—';

    try {
        const res = await fetch(API_URL + '/auth/me', { headers: authHdr() });
        if (!res.ok) { toast('Profil yüklenemedi.'); return; }
        const d   = await res.json();

        document.getElementById('pr-name').textContent   = d.username    || '—';
        document.getElementById('pr-email').textContent  = d.email       || '—';
        document.getElementById('pr-level').textContent  = d.level_name  || 'A1';
        document.getElementById('pr-xp').textContent     = (d.total_xp    ?? 0) + ' XP';
        document.getElementById('pr-streak').textContent = (d.daily_streak ?? 0) + ' gün';
        if (document.getElementById('pr-correct'))
            document.getElementById('pr-correct').textContent = d.total_correct ?? 0;
        if (document.getElementById('pr-sessions'))
            document.getElementById('pr-sessions').textContent = d.total_sessions ?? 0;
    } catch { toast('Bağlantı hatası.'); }
}

let fcQueue = [];
let fcIndex = 0;
let fcSessionId = null;
let fcXpEarned = 0;
let fcCorrectCount = 0;
let fcWrongCount   = 0;

async function loadFlashcards() {
    fcXpEarned = 0; fcCorrectCount = 0; fcWrongCount = 0;
    document.getElementById('fc-summary-overlay').classList.remove('show');
    document.getElementById('fc-card-area').style.display ='flex';
    document.getElementById('fc-empty').style.display = 'none';

    try {
        const res  = await fetch(`${API_URL}/study/queue/${getUserId()}?limit=5`);
        const data = await res.json();
        fcQueue = data.queue || [];

        if (fcQueue.length === 0) {
            document.getElementById('fc-card-area').style.display= 'none';
            document.getElementById('fc-empty').style.display = 'flex';
            return;
        }
        try {
            const sessionRes  = await fetch(API_URL + '/study/session/start', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ user_id: getUserId(), module_type: 'flashcard' })
            });
            const sessionData = await sessionRes.json();
            fcSessionId = sessionData.session_id ?? null;
        } catch { fcSessionId = null; }

        showCard(0);
    } catch {
        toast('Kelimeler yüklenemedi.');
    }
}
async function loadReview() {
    fcXpEarned = 0; fcCorrectCount = 0; fcWrongCount = 0;
    document.getElementById('fc-summary-overlay').classList.remove('show');
    document.getElementById('fc-card-area').style.display = 'flex';
    document.getElementById('fc-empty').style.display= 'none';

    try {
        const res  = await fetch(API_URL + '/study/review', { headers: authHdr() });
        if (!res.ok) { toast('Kelimeler yüklenemedi.'); return; }
        const data = await res.json();
        fcQueue  = (data.words || []).map(w => ({
            word_id: w.id,
            german_word: w.german_word,
            turkish_meaning:     w.turkish_meaning,
            example_sentence_de: w.example_sentence_de,
            example_sentence_tr: w.example_sentence_tr,
            ease_factor:         w.ease_factor,
            repetitions:         w.repetitions
        }));

        if (fcQueue.length ===0) {
            document.getElementById('fc-card-area').style.display = 'none';
            document.getElementById('fc-empty').style.display = 'flex';
            return;
        }

        try {
            const sessionRes  = await fetch(API_URL + '/study/session/start', {
                method: 'POST',
                headers:{ 'Content-Type': 'application/json' },
                body: JSON.stringify({ user_id: getUserId(), module_type: 'review' })
            });
            const sessionData = await sessionRes.json();
            fcSessionId = sessionData.session_id ?? null;
        } catch { fcSessionId = null; }

        showCard(0);
    } catch {
        toast('Kelimeler yüklenemedi.');
    }
}

function showCard(index) {
    fcIndex = index;
    const word = fcQueue[index];

    document.getElementById('fc-word-de').textContent    = word.german_word || '—';
    document.getElementById('fc-word-tr').textContent   = word.turkish_meaning || '';
    document.getElementById('fc-example-de').textContent = word.example_sentence_de || '';
    document.getElementById('fc-example-tr').textContent = word.example_sentence_tr || '';
    document.getElementById('fc-counter').textContent    = `KART ${index + 1}`;
    document.getElementById('fc-progress-fill').style.width = `${Math.round((index / fcQueue.length) * 100)}%`;

    document.getElementById('fc-card-container').classList.remove('flipped');
    document.getElementById('fc-difficulty').classList.remove('show');

    const badge = document.getElementById('fc-difficulty-badge');
    if (badge) {
        if (word.ease_factor !== undefined && word.ease_factor !== null) {
            const ef = word.ease_factor;
            const [label, cls] = ef < 1.5 ? ['Çok Zor', 'very-hard']
                               : ef < 2.0 ? ['Zor',     'hard']
                               :            ['Orta',    'medium'];
            badge.textContent = label;
            badge.className   = `fc-difficulty-badge ${cls}`;
        } else {
            badge.textContent = '';
            badge.className   = 'fc-difficulty-badge';
        }
    }
}

function flipCard() {
    const container = document.getElementById('fc-card-container');
    const flipped   = container.classList.toggle('flipped');
    document.getElementById('fc-difficulty').classList.toggle('show', flipped);
}
async function submitQuality(quality) {
    document.getElementById('fc-difficulty').classList.remove('show');
    const word = fcQueue[fcIndex];
    const body = { user_id: getUserId(), word_id: word.word_id, quality };
    if (fcSessionId) body.session_id = fcSessionId;

    if (quality >= 3) fcCorrectCount++; else fcWrongCount++;

    try {
        const res  = await fetch(API_URL +'/study/answer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        if (res.ok) {
            const data = await res.json();
            fcXpEarned += data.xp_earned ?? 0;
        } else {
            toast('Cevap kaydedilemedi.');
        }
    } catch { toast('Bağlantı hatası.'); }

    const next = fcIndex + 1;
    if (next < fcQueue.length) {
        showCard(next);
    } else {
        document.getElementById('fc-progress-fill').style.width = '100%';
        await finishFlashcardSession();
    }
}

async function finishFlashcardSession() {
    if (fcSessionId) {
        try { await fetch(`${API_URL}/study/session/${fcSessionId}/end`, { method: 'PUT' }); } catch {}
    }
    document.getElementById('fc-summary-cards').textContent = fcQueue.length;
    document.getElementById('fc-summary-xp').textContent    = fcXpEarned;

    const cEl = document.getElementById('fc-summary-correct');
    const wEl = document.getElementById('fc-summary-wrong');
    if (cEl) cEl.textContent = fcCorrectCount;
    if (wEl) wEl.textContent = fcWrongCount;

    const total = fcCorrectCount + fcWrongCount;
    const rateEl = document.getElementById('fc-summary-rate');
    if (rateEl && total > 0) {
        const rate = Math.round((fcCorrectCount / total) * 100);
        document.getElementById('fc-summary-rate-fill').style.width  = rate + '%';
        document.getElementById('fc-summary-rate-label').textContent = rate + '% başarı';
        rateEl.style.display = 'block';
    }

    document.getElementById('fc-summary-overlay').classList.add('show');
}

function playPronunciation() {
    const word = fcQueue[fcIndex];
    if (!word) return;
    const audio = new Audio(`${API_URL}/tts/${encodeURIComponent(word.german_word)}`);
    audio.play().catch(() => toast('Ses oynatılamadı.'));
}

function exitFlashcards() {
    if (!confirm('Oturumdan çıkmak istediğinize emin misiniz?')) return;
    (async () => {
        if (fcSessionId) {
            try { await fetch(`${API_URL}/study/session/${fcSessionId}/end`, { method: 'PUT' }); } catch {}
        }
        window.location.href = '/web/';
    })();
}

const ART_SESSION_LIMIT = 5;
const ART_ARTICLE_NAMES = { 1: 'DER', 2: 'DIE', 3: 'DAS' };

let artWords =[];
let artIndex = 0;
let artSessionId = null;
let artCorrectCount = 0;
let artWrongCount = 0;
let artAnsweredCount = 0;
let artAnswering = false;
let artXpEarned =0;
let artCurrentStreak =0;
let artMaxStreak = 0;
let artDragState = null;

function shuffleArray(arr) {
    const a = arr.slice();
    for (let i = a.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
}

async function loadArtikel() {
    artCorrectCount = 0;
    artWrongCount = 0;
    artAnsweredCount = 0;
    artAnswering = false;
    artXpEarned =0;
    artCurrentStreak = 0;
    artMaxStreak = 0;
    document.getElementById('art-summary-overlay').classList.remove('show');
    document.getElementById('art-progress-fill').style.width='0%';
    initArtikelDrag();

    try {
        const res   = await fetch(API_URL + '/words/?has_article=true');
        const words = await res.json();
        if (!Array.isArray(words) || words.length === 0) { toast('Kelimeler yüklenemedi.'); return; }

        artWords = shuffleArray(words);
        artIndex = 0;
        showArtikelWord();
    } catch { toast('Kelimeler yüklenemedi.'); return; }

    try {
        const sessionRes  = await fetch(API_URL + '/study/session/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ user_id: getUserId(), module_type: 'artikel' })
        });
        const sessionData = await sessionRes.json();
        artSessionId = sessionData.session_id ?? null;
    } catch { artSessionId = null; }
}

function showArtikelWord() {
    const word = artWords[artIndex];
    const card = document.getElementById('art-card');
    document.getElementById('art-word-de').textContent = word.german_word || '—';
    document.getElementById('art-word-tr').textContent = word.turkish_meaning || '';
    document.getElementById('art-counter').textContent =`SORU ${artAnsweredCount + 1}`;
    document.getElementById('art-feedback-badge').innerHTML = '';
    document.getElementById('art-progress-fill').style.width =
        `${Math.round((artAnsweredCount / ART_SESSION_LIMIT) * 100)}%`;
    card.classList.remove('correct', 'wrong');
    card.style.transform = '';
}

function initArtikelDrag() {
    document.querySelectorAll('.art-drop').forEach(chip => {
        chip.addEventListener('pointerdown',  onArtPointerDown);
        chip.addEventListener('pointermove', onArtPointerMove);
        chip.addEventListener('pointerup', onArtPointerUp);
        chip.addEventListener('pointercancel', onArtPointerUp);
    });
}

function onArtPointerDown(e) {
    if (artAnswering) return;
    const chip = e.currentTarget;
    chip.setPointerCapture(e.pointerId);
    chip.classList.add('dragging');
    artDragState ={
        chip,
        articleId: parseInt(chip.dataset.articleId, 10),
        startX: e.clientX,
        startY: e.clientY,
        overTarget: false
    };
}

function onArtPointerMove(e) {
    if (!artDragState) return;
    const chip = artDragState.chip;
    const dx = e.clientX - artDragState.startX;
    const dy = e.clientY - artDragState.startY;
    chip.style.transform = `translate(${dx}px, ${dy}px)`;

    const card = document.getElementById('art-card');
    const r    = card.getBoundingClientRect();
    const over = e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom;
    card.classList.toggle('drag-over', over);
    artDragState.overTarget = over;
}

function onArtPointerUp(e) {
    if (!artDragState) return;
    const chip = artDragState.chip;
    chip.releasePointerCapture(e.pointerId);
    chip.classList.remove('dragging');
    chip.style.transform ='';
    document.getElementById('art-card').classList.remove('drag-over');

    const overTarget = artDragState.overTarget;
    const articleId  = artDragState.articleId;
    artDragState = null;

    if (overTarget) {
        answerArtikel(articleId);
    }
}

function answerArtikel(articleId) {
    if (artAnswering) return;
    artAnswering = true;

    const word  = artWords[artIndex];
    const card  = document.getElementById('art-card');
    const badge = document.getElementById('art-feedback-badge');
    card.style.transform = '';

    const isCorrect = word.article_id === articleId;
    if (isCorrect) {
        artCorrectCount++;
        artCurrentStreak++;
        if (artCurrentStreak > artMaxStreak) artMaxStreak = artCurrentStreak;
        card.classList.add('correct');
        badge.innerHTML = '<svg viewBox="0 0 24 24"><path d="M9,16.2L4.8,12l-1.4,1.4L9,19 21,7l-1.4,-1.4z"/></svg>';
    } else {
        artWrongCount++;
        artCurrentStreak =0;
        card.classList.add('wrong');
        badge.textContent = ART_ARTICLE_NAMES[word.article_id] || '';
    }
    artAnsweredCount++;

    const answerBody = {
        user_id:getUserId(),
        word_id: word.id,
        quality: isCorrect ? 4 : 1
    };
    if (artSessionId) answerBody.session_id = artSessionId;
    fetch(API_URL + '/study/answer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(answerBody)
    }).then(r => r.json()).then(d => { artXpEarned += d.xp_earned || 0; }).catch(() => {});

    setTimeout(() => {
        artAnswering =false;
        if (artAnsweredCount >= ART_SESSION_LIMIT) {
            finishArtikelSession();
        } else {
            artIndex++;
            if (artIndex >= artWords.length) {
                artWords = shuffleArray(artWords);
                artIndex = 0;
            }
            showArtikelWord();
        }
    }, 1200);
}

async function finishArtikelSession() {
    if (artSessionId) {
        try { await fetch(`${API_URL}/study/session/${artSessionId}/end`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ max_streak: artMaxStreak })
        }); } catch {}
    }
    document.getElementById('art-summary-correct').textContent = artCorrectCount;
    document.getElementById('art-summary-wrong').textContent = artWrongCount;
    document.getElementById('art-summary-xp').textContent = artXpEarned;
    document.getElementById('art-summary-overlay').classList.add('show');
}

function exitArtikel() {
    if (!confirm('Oturumdan çıkmak istediğinize emin misiniz?')) return;

    (async () => {
        if (artSessionId) {
            try { await fetch(`${API_URL}/study/session/${artSessionId}/end`, {
                method: 'PUT',
                headers: { 'Content-Type':  'application/json' },
                body: JSON.stringify({ max_streak: artMaxStreak })
            }); } catch {}
        }
        window.location.href = '/web/';
    })();
}

async function loadGrammarList() {
    try {
        const res = await fetch(API_URL + '/grammar/topics');
        if (!res.ok) { toast('Konular yüklenemedi.'); return; }
        const topics = await res.json();

        const a1 = topics.filter(t => t.level === 'A1');
        const a2 = topics.filter(t => t.level === 'A2');

        renderGrammarTopics('gr-list-a1', a1);
        renderGrammarTopics('gr-list-a2', a2);

        const a2Section = document.getElementById('gr-section-a2');
        if (a2Section) a2Section.style.display = a2.length ? '' : 'none';
    } catch { toast('Bağlantı hatası.'); }
}

function renderGrammarTopics(containerId, topics) {
    const el = document.getElementById(containerId);
    if (!el) return;
    if (!topics.length) {el.innerHTML = ''; return; }
    el.innerHTML = topics.map(t => `
        <button class="gr-topic-card" onclick="window.location.href='/web/grammar/${t.slug}'">
            <div class="gr-topic-icon">
                <svg viewBox="0 0 24 24"><path d="M18,2h-12c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM6,4h5v8l-2.5,-1.5L6,12V4z"/></svg>
            </div>
            <div class="gr-topic-info">
                <div class="gr-topic-title">${t.title}</div>
                <span class="gr-level-badge">${t.level}</span>
            </div>
            <svg class="gr-topic-arrow" viewBox="0 0 24 24"><path d="M8.59,16.59L13.17,12 8.59,7.41 10,6l6,6 -6,6z"/></svg>
        </button>
    `).join('');
}

let grExercises = [];
let grIndex = 0;
let grCorrect = 0;
let grWrong = 0;
let grXpEarned = 0;
let grAnswered = false;
let grSessionId = null;
let grCurrentStreak = 0;
let grMaxStreak = 0;

async function loadGrammarDetail() {
    const parts = window.location.pathname.split('/').filter(Boolean);
    const slug = parts[parts.length - 1];

    try {
        const res = await fetch(`${API_URL}/grammar/topics/${slug}/exercises`);
        if (!res.ok) { toast('Konu yüklenemedi.'); return; }
        const data = await res.json();

        document.getElementById('gr-title').textContent = data.title;
        document.getElementById('gr-explain-content').innerHTML = data.explanation || '';
        grExercises =data.exercises || [];

        document.getElementById('gr-explain-view').style.display = 'flex';
        document.getElementById('gr-exercise-view').style.display = 'none';
    } catch { toast('Bağlantı hatası.'); }
}

async function startGrammarExercises() {
    if (!grExercises.length){ toast('Bu konuda henüz alıştırma yok.'); return; }
    grIndex = 0;
    grCorrect = 0;
    grWrong = 0;
    grXpEarned = 0;
    grCurrentStreak = 0;
    grMaxStreak = 0;

    try {
        const sessionRes = await fetch(API_URL + '/study/session/start', {
            method: 'POST',
            headers:{ 'Content-Type': 'application/json' },
            body: JSON.stringify({ user_id: getUserId(), module_type: 'grammar' })
        });
        const sessionData =await sessionRes.json();
        grSessionId = sessionData.session_id ?? null;
    } catch { grSessionId = null; }

    document.getElementById('gr-explain-view').style.display = 'none';
    document.getElementById('gr-exercise-view').style.display = 'flex';
    document.getElementById('gr-progress-track').style.display = 'block';
    document.getElementById('gr-counter').style.display = '';

    showGrammarExercise();
}

function showGrammarExercise() {
    const ex = grExercises[grIndex];
    grAnswered = false;

    const total = grExercises.length;
    document.getElementById('gr-counter').textContent = `SORU ${grIndex + 1}/${total}`;
    document.getElementById('gr-progress-fill').style.width =
        `${Math.round((grIndex / total) * 100)}%`;

    document.getElementById('gr-question-text').textContent = ex.question;

    const feedback = document.getElementById('gr-feedback');
    feedback.className = 'gr-feedback';
    feedback.innerHTML = '';

    document.getElementById('gr-next-btn').style.display = 'none';

    const fillArea = document.getElementById('gr-fill-area');
    const choiceArea = document.getElementById('gr-choice-area');
    const submitBtn  = document.getElementById('gr-submit-btn');

    if (ex.exercise_type === 'fill_blank') {
        fillArea.style.display = 'block';
        choiceArea.style.display = 'none';
        submitBtn.style.display = 'block';
        const inp = document.getElementById('gr-fill-input');
        inp.value = '';
        inp.disabled = false;
        setTimeout(() => inp.focus(), 100);
    } else {
        fillArea.style.display = 'none';
        choiceArea.style.display = 'grid';
        submitBtn.style.display = 'none';
        const opts = ex.options || [];
        choiceArea.innerHTML = opts.map(opt =>
            `<button class="gr-option-btn" data-answer="${opt.replace(/&/g,'&amp;').replace(/"/g,'&quot;')}">${opt}</button>`
        ).join('');
        choiceArea.querySelectorAll('.gr-option-btn').forEach(btn => {
            btn.addEventListener('click', () => submitGrammarChoice(btn.dataset.answer, btn));
        });
    }
}

async function submitGrammarFill() {
    if (grAnswered) return;
    const inp = document.getElementById('gr-fill-input');
    const answer = inp.value.trim();
    if (!answer) { toast('Bir cevap girin.'); return; }
    await doGrammarCheck(answer, null);
}

async function submitGrammarChoice(answer, btn) {
    if (grAnswered) return;
    await doGrammarCheck(answer, btn);
}

async function doGrammarCheck(answer, clickedBtn) {
    grAnswered = true;
    const ex = grExercises[grIndex];

    try {
        const res = await fetch(API_URL + '/grammar/check', {
            method: 'POST',
            headers: authHdr(),
            body: JSON.stringify({ exercise_id: ex.id, answer, user_id: parseInt(getUserId()) || null })
        });
        if (!res.ok) { grAnswered = false; toast('Sunucu hatası.'); return; }
        const data = await res.json();

        if (data.correct) {
            grCorrect++; grXpEarned += data.xp_earned || 0;
            grCurrentStreak++;
            if (grCurrentStreak >grMaxStreak) grMaxStreak = grCurrentStreak;
        } else {
            grWrong++;
            grCurrentStreak = 0;
        }

        if (clickedBtn) {
            clickedBtn.classList.add(data.correct ? 'correct' : 'wrong');
            if (!data.correct) {
                document.querySelectorAll('.gr-option-btn').forEach(b => {
                    if (b.dataset.answer === data.correct_answer) b.classList.add('correct');
                });
            }
            document.querySelectorAll('.gr-option-btn').forEach(b => { b.onclick = null; b.style.pointerEvents = 'none'; });
        } else {
            document.getElementById('gr-fill-input').disabled = true;
            document.getElementById('gr-submit-btn').style.display = 'none';
        }

        const feedback = document.getElementById('gr-feedback');
        feedback.className = 'gr-feedback ' + (data.correct ? 'correct' : 'wrong');
        const resultLine = data.correct
            ? '✓ Doğru!'
            : `✗ Yanlış &mdash; Doğru cevap: <strong>${data.correct_answer}</strong>`;
        const explainLine = data.explanation
            ? `<div class="gr-feedback-explain">${data.explanation}</div>` : '';
        feedback.innerHTML= `<div class="gr-feedback-result">${resultLine}</div>${explainLine}`;

        const nextBtn = document.getElementById('gr-next-btn');
        nextBtn.textContent = (grIndex + 1 >= grExercises.length) ? 'Tamamla' : 'Sonraki Soru';
        nextBtn.style.display = 'block';

    } catch { grAnswered = false; toast('Bağlantı hatası.'); }
}

async function nextGrammarQuestion() {
    grIndex++;
    if (grIndex >= grExercises.length) {
        if (grSessionId) {
            try { await fetch(`${API_URL}/study/session/${grSessionId}/end`, {
                method:'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ max_streak: grMaxStreak })
            }); } catch {}
            grSessionId = null;
        }
        document.getElementById('gr-exercise-view').style.display = 'none';
        document.getElementById('gr-progress-fill').style.width = '100%';
        document.getElementById('gr-summary-correct').textContent = grCorrect;
        document.getElementById('gr-summary-wrong').textContent = grWrong;
        document.getElementById('gr-summary-xp').textContent = grXpEarned;
        document.getElementById('gr-summary-overlay').classList.add('show');
    } else {
        showGrammarExercise();
    }
}
function exitGrammarDetail() {
    const inExercise =document.getElementById('gr-exercise-view') &&
                       document.getElementById('gr-exercise-view').style.display !== 'none';
    if (inExercise && !confirm('Alıştırmadan çıkmak istediğinize emin misiniz?')) return;
    if (grSessionId) {
        fetch(`${API_URL}/study/session/${grSessionId}/end`, { method: 'PUT' }).catch(() => {});
        grSessionId =null;
    }
    window.location.href = '/web/grammar';
}