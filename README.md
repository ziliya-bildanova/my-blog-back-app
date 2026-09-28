# My Blog Backend App

Backend for the [my-blog-front-app](../my-blog-front-app) frontend (blog лента постов + страница поста).

- **Java 21**, **Spring Framework 6.1** (без Spring Boot), сборка **Maven**, упаковка **WAR**
- Сервлет-контейнер: **Tomcat 10.1** (подойдёт и Jetty 12, см. деплой)
- БД: **H2 in-memory** (схема создаётся при старте из `schema.sql`, демо-данные — из `data.sql`);
  параметры подключения — в `application.properties`, переопределяются через `-Ddb.url=... -Ddb.user=... -Ddb.password=...`
- Слои: `controller` → `service` → `dao` (JdbcTemplate) → H2; DTO для REST-контракта с Bean Validation;
  `GlobalExceptionHandler` (404/400/413/415 в JSON)
- Тесты: JUnit 5 + Spring TestContext (один кешированный контекст на все Spring-тесты), Mockito, MockMvc, AssertJ

## Быстрый старт

Требования: JDK 21+, Maven 3.9+, Docker (опционально), Tomcat 10.1 (для ручного деплоя).

```sh
git clone <your-fork-url> my-blog-back-app
cd my-blog-back-app
git checkout develop

# тесты (54 теста, всё зелёное)
mvn test

# сборка war
mvn package   # -> target/my-blog-back-app.war
```

Backend слушает **http://localhost:8080**, фронт по умолчанию обращается именно туда.

## Запуск в Docker (рекомендуется для связки с фронтом)

```sh
docker build -t my-blog-back-app .
docker run --rm -p 8080:8080 my-blog-back-app
```

Проверка: `curl "http://localhost:8080/api/posts?search=&pageNumber=1&pageSize=5"`

## Деплой в Tomcat 10.1 вручную

1. Удалите дефолтное приложение (иначе оно перекроет `ROOT.war`):
   `rm -rf $CATALINA_HOME/webapps/ROOT`
2. `cp target/my-blog-back-app.war $CATALINA_HOME/webapps/ROOT.war`
3. `$CATALINA_HOME/bin/startup.sh`

WAR должен разворачиваться как **ROOT**, потому что фронт ходит на `http://localhost:8080/api/...`
без префикса контекста. В Jetty 12 аналогично: `cp ...war $JETTY_BASE/webapps/root.war`.

Контекст Spring поднимается через `WebAppInitializer`
(`AbstractAnnotationConfigDispatcherServletInitializer`, конфиг `AppConfig`);
`src/main/webapp/WEB-INF/web.xml` — минимальный дескриптор (Servlet 6.0).

## Тесты

```sh
mvn test
```

- `dao/*Test` — DAO на встроенной H2 (кешированный контекст `TestConfig`)
- `service/*Test` — сервисы + реальный DAO-слой на H2 (тот же контекст)
- `controller/*MvcTest` — MVC-срезы: контроллеры + Jackson + валидация + handler, сервисы замоканы (без контекста)
- `BlogIntegrationTest` — сквозной сценарий по HTTP со всеми полями ответов (тот же контекст)
- `BlogErrorTest` — ошибки: валидация, отсутствующие ресурсы, битый JSON, лимиты (тот же контекст)
- `ConcurrentCommentTest` — гонка «удаление поста vs создание комментария» (тот же контекст)

Один общий контекст на все Spring-тесты (`BaseSpringTest`), каждый тест в транзакции с rollback —
кеширование Spring TestContext, изоляция без чистки таблиц.

## Структура

```
src/main/java/com/example/blog/
  config/AppConfig.java               # сборка: WebMvcConfig + PersistenceConfig + DbInitConfig
  config/WebMvcConfig.java            # MVC, CORS, multipart
  config/PersistenceConfig.java       # DataSource (из application.properties), JdbcTemplate, транзакции
  config/DbInitConfig.java            # накатка schema.sql + data.sql при старте
  config/WebAppInitializer.java       # bootstrap DispatcherServlet -> "/", лимиты multipart
  controller/PostController.java      # /api/posts + likes + image (параметры обязательны, @Valid)
  controller/CommentController.java   # /api/posts/{postId}/comments (@Valid)
  service/PostService(Impl).java      # пагинация (pageSize ≤ 100), сверка id, лимит картинки 5 МБ
  service/CommentService(Impl).java   # сверка id/postId, вставка комментария одним SQL
  dao/PostDao(Impl).java             # JdbcTemplate: превью в SQL, batch тегов, регистронезависимый поиск с ESCAPE
  dao/CommentDao(Impl).java          # INSERT..SELECT (без гонки), boolean/Optional вместо исключений
  model/Post.java, Comment.java, ImageData.java   # defensive copy, equals/hashCode по id
  dto/PostDto.java, PostListResponse.java, Create/UpdatePostRequest.java,
      CommentDto.java, Create/UpdateCommentRequest.java, PostValidation.java  # Bean Validation
  exception/NotFoundException.java, PayloadTooLargeException.java, GlobalExceptionHandler.java
src/main/resources/application.properties  # db.url/db.user/db.password (можно -Ddb.url=...)
src/main/resources/schema.sql         # posts, post_tags (PK post_id+tag), comments (CASCADE)
src/main/resources/data.sql           # демо-данные для ручной проверки с фронтом
```

Схема БД: `posts(id, title VARCHAR(255), text, likes_count, image, image_content_type)`,
`post_tags(post_id → posts ON DELETE CASCADE, tag VARCHAR(100), PK(post_id, tag))`,
`comments(id, post_id → posts ON DELETE CASCADE, text VARCHAR(2000))`.

Правила валидации: title `@NotBlank @Size(255)`, текст поста `@NotBlank`,
тег `@NotBlank @Size(100)`, не более 10 тегов, комментарий `@NotBlank @Size(2000)`,
id/postId в теле обязаны совпадать с URL. Поиск регистронезависимый, `%` и `_` ищутся буквально.
Картинка — макс. 5 МБ (иначе 413), пустой файл — 400.

## API

| Метод | URL | Тело | Ответ |
|---|---|---|---|
| GET | `/api/posts?search=S&pageNumber=1&pageSize=5` | — | `{posts:[{id,title,text(≤128+…),tags,likesCount,commentsCount}], hasPrev, hasNext, lastPage}` |
| GET/POST | `/api/posts/{id}` | — | `{id,title,text(полный),tags,likesCount,commentsCount}` (POST — для совместимости с текстом задания; фронт использует GET) |
| POST | `/api/posts` | `{title,text,tags}` | созданный пост (`likesCount=0, commentsCount=0`) |
| PUT | `/api/posts/{id}` | `{id,title,text,tags}` | обновлённый пост |
| DELETE | `/api/posts/{id}` | — | `200 OK` (комментарии удаляются каскадно) |
| POST | `/api/posts/{id}/likes` | — | новое число лайков (plain text) |
| PUT | `/api/posts/{id}/image` | `multipart/form-data`, поле `image` | `200 OK` |
| GET | `/api/posts/{id}/image` | — | байты картинки (404, если нет) |
| GET | `/api/posts/{id}/comments` | — | `[{id,text,postId}]` |
| GET | `/api/posts/{id}/comments/{commentId}` | — | `{id,text,postId}` |
| POST | `/api/posts/{id}/comments` | `{text,postId}` | созданный комментарий |
| PUT | `/api/posts/{id}/comments/{commentId}` | `{id,text,postId}` | обновлённый комментарий |
| DELETE | `/api/posts/{id}/comments/{commentId}` | — | `200 OK` |

Ошибки: `404 {"error": "..."}` (нет поста/комментария/картинки),
`400 {"error": "..."}` (валидация, неверные id/параметры/JSON),
`413 {"error": "..."}` (картинка больше 5 МБ), `415` (неверный Content-Type).
CORS открыт (`*`) — фронт (nginx на `:80`) ходит на `:8080` с другого ориджина.

Примеры:

```sh
curl "http://localhost:8080/api/posts?search=Lalala&pageNumber=1&pageSize=5"
curl -X POST http://localhost:8080/api/posts -H 'Content-Type: application/json' \
  -d '{"title":"Пост 3","text":"Текст **markdown**","tags":["tag_1"]}'
curl -X POST http://localhost:8080/api/posts/3/likes
curl -X PUT http://localhost:8080/api/posts/3/image -F "image=@pic.jpg;type=image/jpeg"
```

## Git-процесс (GitFlow)

Работа велась в ветке `develop` микрокоммитами, слияние в `main` — через merge.
Чтобы опубликовать у себя:

```sh
# создайте пустой публичный репозиторий my-blog-back-app на GitHub, затем:
git remote add origin git@github.com:<you>/my-blog-back-app.git
git push -u origin main develop
```
