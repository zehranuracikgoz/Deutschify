import os
import sys
import json
import glob

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from backend.database import get_db


def seed_grammar():
    content_dir =os.path.join(os.path.dirname(os.path.abspath(__file__)), 'backend', 'grammar_content')
    json_files = sorted(glob.glob(os.path.join(content_dir, '*.json')))

    if not json_files:
        print('Hata:backend/grammar_content/ klasöründe JSON dosyası bulunamadı.')
        sys.exit(1)

    print(f'{len(json_files)} dosya bulundu, yükleniyor...\n')

    with get_db() as conn:
        cursor = conn.cursor()

        for filepath in json_files:
            filename =os.path.basename(filepath)

            with open(filepath, encoding='utf-8') as f:
                data = json.load(f)

            topic_data = data['topic']
            exercises = data.get('exercises', [])

            cursor.execute('''
                INSERT INTO grammar_topics (title, slug, level, explanation, display_order)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (slug) DO UPDATE SET
                    title = EXCLUDED.title,
                    level = EXCLUDED.level,
                    explanation   = EXCLUDED.explanation,
                    display_order = EXCLUDED.display_order
                RETURNING id
            ''', (
                topic_data['title'],
                topic_data['slug'],
                topic_data['level'],
                topic_data['explanation'],
                topic_data.get('display_order', 0)
            ))
            topic_id = cursor.fetchone()[0]

            cursor.execute('DELETE FROM grammar_exercises WHERE topic_id = %s', (topic_id,))

            for ex in exercises:
                options_json = json.dumps(ex['options'], ensure_ascii=False) if ex.get('options') else None
                cursor.execute('''
                    INSERT INTO grammar_exercises
                        (topic_id, question, exercise_type, options, correct_answer, explanation, display_order)
                    VALUES (%s, %s, %s, %s::jsonb, %s, %s, %s)
                ''', (
                    topic_id,
                    ex['question'],
                    ex['type'],
                    options_json,
                    ex['correct_answer'],
                    ex.get('explanation', ''),
                    ex.get('display_order', 0)
                ))

            print(f'  ✓  {filename}  ->  {topic_data["slug"]}  ({len(exercises)} soru)')

    print('\nGrammar içeriği başarıyla yüklendi.')


if __name__ == '__main__':
    seed_grammar()