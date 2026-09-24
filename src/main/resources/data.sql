-- Demo data (used on application startup, NOT in tests)
INSERT INTO posts (id, title, text, likes_count) VALUES
    (1, 'Первый пост', 'Добро пожаловать в блог! Это демонстрационный пост.', 5),
    (2, 'Markdown', '# Заголовок

Пример текста поста в формате **Markdown**.', 1);

ALTER TABLE posts ALTER COLUMN id RESTART WITH 3;

INSERT INTO post_tags (post_id, tag) VALUES
    (1, 'tag_1'),
    (1, 'tag_2');

INSERT INTO comments (id, post_id, text) VALUES
    (1, 1, 'Первый комментарий к посту 1'),
    (2, 2, 'Комментарий к посту 2');

ALTER TABLE comments ALTER COLUMN id RESTART WITH 3;
