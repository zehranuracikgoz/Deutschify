from backend.database import get_db

def seed_data():
    with get_db() as conn:
        conn.execute("INSERT OR IGNORE INTO articles (name) VALUES ('der')")
        conn.execute("INSERT OR IGNORE INTO articles (name) VALUES ('die')")
        conn.execute("INSERT OR IGNORE INTO articles (name) VALUES ('das')")
        conn.execute("INSERT OR IGNORE INTO word_categories (name) VALUES ('Temel Kelimeler')")
        conn.execute("INSERT OR IGNORE INTO word_types (name) VALUES ('Noun')")

        words = [
            ('Hund', 'Köpek', 'Der Hund ist groß.', 'Köpek büyük.', 1, 1, 1),
            ('Katze', 'Kedi', 'Die Katze schläft.', 'Kedi uyuyor.', 2, 1, 1),
            ('Buch', 'Kitap', 'Das Buch ist interessant.', 'Kitap ilginç.', 3, 1, 1),
            ('Haus', 'Ev', 'Das Haus ist groß.', 'Ev büyük.', 3, 1, 1),
            ('Schule', 'Okul', 'Die Schule ist neu.', 'Okul yeni.', 2, 1, 1),
        ]
        conn.executemany(
            '''INSERT OR IGNORE INTO words
            (german_word, turkish_meaning, example_sentence_de, example_sentence_tr, article_id, category_id, type_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)''',
            words
        )
    print("Veriler başarıyla eklendi.")

if __name__ == '__main__':
    seed_data()