**Dil / Language:** [🇹🇷 Türkçe](#türkçe) · [🇬🇧 English](#english)
---
<a name="türkçe"></a>

# Deutschify — Almanca Öğrenme Uygulaması

Deutschify, Türkçe konuşanlar için tasarlanmış bir Android Almanca öğrenme uygulamasıdır. Flashcard, sürükle-bırak artikel eşleştirme ve gramer egzersizleri aracılığıyla kullanıcıların kelime dağarcığını ve dil bilgisini geliştirmesini hedefler. Yaptığı hataları kaydeder ve tekrar modülüyle pekiştirme sağlar.

## Teknolojiler

- Java · Android SDK
- SQLite
- Android TTS (Text-to-Speech)
- RecyclerView · CardView

## Özellikler

- **Flashcard** — 10 rastgele kelime, kart çevirme animasyonu ve Almanca sesli okuma
- **Swiper** — Kelimeleri der / die / das hedeflerine sürükleyerek artikel eşleştirme
- **Gramer** — Präsens, Perfekt, Präteritum ve diğer zamanlar için ilerleme takibi
- **Hata Tekrarı** — Yanlış cevaplanan kelimeler kaydedilir ve ayrı modülde tekrar edilir
- **Sonuç Ekranı** — Her oturum sonunda başarı yüzdesi ve doğru / yanlış sayısı

## Ekranlar

> Ekran görüntüleri yakında eklenecek.

| Ekran | Açıklama |
|---|---|
| Ana Menü | Dört modüle erişim: Flashcard, Swiper, Gramer, Tekrar |
| Flashcard | Kart çevirme ve zorluk derecelendirme (1–2–3) |
| Swiper | Sürükle-bırak artikel sınıflandırma |
| Gramer | Zaman bazlı ilerleme çubukları |
| Sonuç | Oturum sonu istatistikleri |
| Hata Listesi | Tüm hataların RecyclerView ile listelenmesi |
| Hata Tekrarı | Hatalar için interaktif flashcard veya swiper arayüzü |

## Projeyi Çalıştırma

```
# Repoyu klonla
git clone https://github.com/zehranuracikgoz/deutschify-mobile.git

# Android Studio ile aç
File > Open > deutschify-mobile klasörünü seç

# Emülatör veya fiziksel cihazda çalıştır
Run > Run 'app'
```

**Gereksinimler:** Android Studio · Android SDK 24+

## Veri Yapısı

- `words.json` — Almanca kelimeler, artikeller ve Türkçe karşılıkları
- `SQLite` — Kelimeler, gramer soruları ve hata geçmişi üç ayrı tabloda tutulur

## Not

Bu uygulama akademik ve kişisel gelişim amaçlıdır.

---

<a name="english"></a>

# Deutschify — German Learning App

Deutschify is an Android application designed for Turkish speakers learning German. Through flashcards, drag-and-drop article matching, and grammar exercises, it helps users build vocabulary and improve language skills. Mistakes are recorded and revisited through a dedicated revision module.

## Technologies

- Java · Android SDK
- SQLite
- Android TTS (Text-to-Speech)
- RecyclerView · CardView

## Features

- **Flashcard** — 10 random words with flip animation and German text-to-speech
- **Swiper** — Drag words to der / die / das targets to practice articles
- **Grammar** — Progress tracking for Präsens, Perfekt, Präteritum and other tenses
- **Revision** — Incorrect answers are saved and revisited in a dedicated module
- **Results Screen** — Success percentage and correct / incorrect counts after each session

## Screens

> Screenshots coming soon.

| Screen | Description |
|---|---|
| Main Menu | Access to four modules: Flashcard, Swiper, Grammar, Revision |
| Flashcard | Card flip with difficulty rating (1–2–3) |
| Swiper | Drag-and-drop article classification |
| Grammar | Tense-based progress bars |
| Result | End-of-session statistics |
| Mistake List | All mistakes listed with RecyclerView |
| Revision | Interactive flashcard or swiper interface for mistakes |

## Running the Project

```
# Clone the repo
git clone https://github.com/zehranuracikgoz/deutschify-mobile.git

# Open in Android Studio
File > Open > select the deutschify-mobile folder

# Run on emulator or physical device
Run > Run 'app'
```

**Requirements:** Android Studio · Android SDK 24+

## Data Structure

- `words.json` — German words with articles and Turkish translations
- `SQLite` — Words, grammar questions, and mistake history stored in three separate tables

## Note

This application is intended for personal learning and academic purposes.
