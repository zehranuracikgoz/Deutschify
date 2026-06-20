**Dil / Language:** [🇹🇷 Türkçe](#türkçe) · [🇬🇧 English](#english)

---

<a name="türkçe"></a>

# Deutschify — Almanca Öğrenme Uygulaması

Deutschify, Türk kullanıcılar için A1/A2 seviyesi Almanca öğrenmeyi hedefleyen tam yığın bir uygulamadır. SM-2 aralıklı tekrar algoritması, Google Gemini destekli AI hata analizi ve deterministik günlük kelime seçimi içerir. Flask REST API, Android (Java MVVM) ve web arayüzünden oluşur.

---

## Teknolojiler

- **Python 3.11, Flask 3, PostgreSQL**
- **Flask-JWT-Extended**
- **Google Gemini API**
- **Hugging Face Transformers (T5)** — bağlamsal örnek cümle üretimi
- **gTTS**
- **Android Java, MVVM, Retrofit 2**
- **pytest**
- **Docker, Render**

---

## Özellikler

- SM-2 algoritmasıyla aralıklı tekrar (SRS)
- Artikel testi — der/die/das + AI hata açıklaması
- Dil bilgisi alıştırmaları (web: AI geri bildirimli, Android: geliştirme aşamasında)
- Bağlamsal örnek cümle üretimi (T5)
- Günün kelimesi — deterministik günlük seçim
- Haftalık aktivite, XP ve streak takibi
- JWT tabanlı kimlik doğrulama

---

## Süreç

Proje, temel API iskeleti ve kimlik doğrulamayla (JWT) başladı; ardından SM-2 algoritması bağımsız, dış bağımlılıksız bir modül olarak eklendi. Android istemcisi MVVM deseniyle Retrofit üzerinden aynı API'ye bağlanır. Günün kelimesi seçimi yılın günü modulo toplam kelime sayısı formülüyle hesaplanır; böylece tüm kullanıcılar aynı günde aynı kelimeyi görür. AI hata analizi Google Gemini, örnek cümle üretimi Hugging Face T5 üzerinden çalışır.

---

## Kurulum

```bash
pip install -r requirements.txt
# .env: DATABASE_URL, SECRET_KEY, JWT_SECRET_KEY, GEMINI_API_KEY, HF_TOKEN

docker-compose up -d   # PostgreSQL container'ını başlatır
python seed.py         # veritabanını doldurur
python app.py          # Flask uygulamasını çalıştırır
```

### Testler

```bash
pytest tests/ -v
```

---

---

<a name="english"></a>

# Deutschify — German Learning Application

Deutschify is a full-stack application for A1/A2 level German learning targeted at Turkish users. It features the SM-2 spaced repetition algorithm, Google Gemini-powered AI error analysis, and deterministic daily word selection. Built with Flask REST API, Android (Java MVVM), and a web interface.

---

## Technologies

- **Python 3.11, Flask 3, PostgreSQL**
- **Flask-JWT-Extended**
- **Google Gemini API**
- **Hugging Face Transformers (T5)** — contextual example sentence generation
- **gTTS**
- **Android Java, MVVM, Retrofit 2**
- **pytest**
- **Docker, Render**

---

## Features

- Spaced repetition with SM-2 algorithm (SRS)
- Artikel quiz — der/die/das + AI error explanation
- Grammar exercises (web: AI feedback, Android: in development)
- Contextual example sentence generation (T5)
- Word of the day — deterministic daily selection
- Weekly activity, XP and streak tracking
- JWT-based authentication

---

## The Process

The project started with the core API skeleton and authentication (JWT); SM-2 was then added as a standalone, dependency-free module. The Android client connects to the same API via Retrofit using the MVVM pattern. Daily word selection is computed via day-of-year modulo total word count, ensuring all users see the same word on the same day. AI error analysis runs through Google Gemini; example sentence generation through Hugging Face T5.

---

## Installation

```bash
pip install -r requirements.txt
# .env: DATABASE_URL, SECRET_KEY, JWT_SECRET_KEY, GEMINI_API_KEY, HF_TOKEN

docker-compose up -d   # starts PostgreSQL container
python seed.py         # populates the database
python app.py          # starts the Flask application
```

### Tests

```bash
pytest tests/ -v
```
