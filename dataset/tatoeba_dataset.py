"""
Tatoeba'dan Almanca örnek cümleler çekip t5_train.json oluşturuyor

komut
    python dataset/tatoeba_dataset.py

çıktı
    dataset/t5_train.json
"""

import bz2
import io
import json
import os
import re
import sqlite3
import tarfile
import urllib.request
from collections import defaultdict

TATOEBA_URL = "https://downloads.tatoeba.org/exports/sentences.tar.bz2"
DB_PATH = os.path.join(os.path.dirname(__file__), "..", "instance", "deutschify.db")
OUT_PATH = os.path.join(os.path.dirname(__file__), "t5_train.json")

MIN_WORDS = 4
MAX_WORDS = 12
MAX_PER_WORD = 5


def load_words(db_path: str) -> list[str]:
    with sqlite3.connect(db_path) as conn:
        rows = conn.execute("SELECT german_word FROM words").fetchall()
    return [r[0] for r in rows]


def is_valid_sentence(sentence: str) -> bool:
    sentence = sentence.strip()
    if not sentence:
        return False
    if sentence[-1] not in ".?!":
        return False
    word_count = len(sentence.split())
    return MIN_WORDS <= word_count <= MAX_WORDS


def build_word_pattern(word: str) -> re.Pattern:
    escaped = re.escape(word)
    return re.compile(rf"\b{escaped}\b", re.IGNORECASE)


def download_and_filter(url: str, words: list[str]) -> dict[str, list[str]]:
    patterns = {w: build_word_pattern(w) for w in words}
    buckets: dict[str, list[str]] = defaultdict(list)

    print(f"İndiriliyor: {url}")
    print("(Bu işlem birkaç dakika sürebilir — dosya ~1 GB)")

    with urllib.request.urlopen(url) as response:
        fileobj = io.BytesIO(response.read())

    print("İndirme tamamlandı. Cümleler filtreleniyor...")

    with tarfile.open(fileobj=fileobj, mode="r:bz2") as tar:
        for member in tar.getmembers():
            if not member.name.endswith(".csv"):
                continue
            f = tar.extractfile(member)
            if f is None:
                continue

            for raw_line in f:
                try:
                    line = raw_line.decode("utf-8").rstrip("\n")
                except UnicodeDecodeError:
                    continue

                parts = line.split("\t", 2)
                if len(parts) != 3:
                    continue

                _, lang, sentence = parts
                if lang != "deu":
                    continue
                if not is_valid_sentence(sentence):
                    continue

                for word, pattern in patterns.items():
                    if len(buckets[word]) >= MAX_PER_WORD:
                        continue
                    if pattern.search(sentence):
                        buckets[word].append(sentence.strip())

    return buckets


def build_dataset(words: list[str], buckets: dict[str, list[str]]) -> list[dict]:
    records = []
    for word in words:
        for sentence in buckets.get(word, []):
            records.append({
                "input": f"Beispielsatz mit dem Wort: {word}",
                "output": sentence,
            })
    return records


def print_summary(words: list[str], buckets: dict[str, list[str]]) -> None:
    found = sum(1 for w in words if buckets.get(w))
    total_sentences = sum(len(v) for v in buckets.values())
    missing = [w for w in words if not buckets.get(w)]

    print(f"\n--- ÖZET ---")
    print(f"Toplam kelime       : {len(words)}")
    print(f"Cümle bulunan       : {found}")
    print(f"Cümle bulunamayan   : {len(missing)}")
    print(f"Toplam kayıt (JSON) : {total_sentences}")
    if missing:
        print(f"Bulunamayan kelimeler: {', '.join(missing)}")


def main() -> None:
    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)

    print("Veritabanından kelimeler yükleniyor...")
    words = load_words(DB_PATH)
    print(f"{len(words)} kelime yüklendi.")

    buckets = download_and_filter(TATOEBA_URL, words)
    dataset = build_dataset(words, buckets)

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(dataset, f, ensure_ascii=False, indent=2)

    print(f"\nKaydedildi: {OUT_PATH}")
    print_summary(words, buckets)


if __name__ == "__main__":
    main()