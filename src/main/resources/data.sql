-- Demo data (used on application startup, NOT in tests)
INSERT INTO posts (id, title, text, likes_count) VALUES
    (1, 'Первый пост', 'Добро пожаловать в блог!', 5),
    (2, 'Канье Уест все таки приедет в Питер', 'но это не точно', 1);

ALTER TABLE posts ALTER COLUMN id RESTART WITH 3;

INSERT INTO post_tags (post_id, tag) VALUES
    (2, 'новости'),
    (1, 'мем');

INSERT INTO comments (id, post_id, text) VALUES
    (1, 1, 'Очень смешно! хахах'),
    (2, 2, 'не верю');

ALTER TABLE comments ALTER COLUMN id RESTART WITH 3;
