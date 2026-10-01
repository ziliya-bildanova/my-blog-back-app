# My Blog Backend App (Spring Boot)

Backend for the [my-blog-front-app](../my-blog-front-app) frontend (blog лента постов + страница поста).
Continuation of the plain-Spring project: same REST contract and layers, now on **Spring Boot 3.4 + Gradle**.

- **Java 21**, **Spring Boot 3.4** (embedded Tomcat), сборка **Gradle**, упаковка **Executable Jar**
- БД: **H2 in-memory** (схема из `schema.sql`, демо-данные из `data.sql` накатываются Boot автоматически);
  параметры подключения — в `application.properties`, переопределяются через `-Dspring.datasource.url=...`
- Слои: `controller` → `service` → `dao` (JdbcTemplate) → H2; DTO с Bean Validation;
  `GlobalExceptionHandler` (404/400/413/415 в JSON)
- Тесты: JUnit 5 + Spring Boot Test (один кешированный контекст на все full-тесты), `@WebMvcTest`-срезы, Mockito, MockMvc, AssertJ

## Быстрый старт

Требования: JDK 21+, Gradle 8.10+ (или `./gradlew`, если добавлен wrapper).

```sh
git clone <your-fork-url> my-blog-back-app
cd my-blog-back-app
git checkout module_one_sprint_four_branch

# тесты (всё зелёное)
gradle test

# сборка executable jar
gradle bootJar   # -> build/libs/my-blog-back-app-1.0.0.jar

# запуск (встроенный Tomcat на :8080)
java -jar build/libs/my-blog-back-app-1.0.0.jar
```

Backend слушает **http://localhost:8080**, фронт по умолчанию обращается именно туда.

## Запуск в Docker (рекомендуется для связки с фронтом)

```sh
docker build -t my-blog-back-app .
docker run --name my-blog-back-app --restart unless-stopped -p 8080:8080 my-blog-back-app
```

Проверка: `curl "http://localhost:8080/api/posts?search=&pageNumber=1&pageSize=5"`

## Тесты

```sh
gradle test
```

- `dao/*Test`, `service/*Test` — `@SpringBootTest` на встроенной H2 (общий кешированный контекст)
- `controller/*MvcTest` — `@WebMvcTest`-срезы: контроллеры + Jackson + валидация, сервисы — `@MockitoBean`
- `BlogIntegrationTest` — сквозной сценарий по HTTP со всеми полями ответов
- `BlogErrorTest` — ошибки: валидация, отсутствующие ресурсы, битый JSON, лимиты
- `ConcurrentCommentTest` — гонка «удаление поста vs создание комментария»
- `service/ToPreviewTest` — чистый юнит без контекста

Тестовые ресурсы (`src/test/resources`): своя H2 (`testdb`), только `schema.sql`, пустой `data.sql` — демо-данные в тесты не попадают. Каждый тест в транзакции с rollback.

## Структура

```
src/main/java/com/example/blog/
  MyBlogBackApplication.java      # точка входа (@SpringBootApplication)
  config/WebConfig.java           # CORS (без @EnableWebMvc, чтобы не гасить авто-конфигурацию Boot)
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
src/main/resources/application.properties  # порт, DataSource, multipart-лимиты
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

Ветка спринта `module_one_sprint_four_branch` от `main`, микрокоммиты, затем Pull Request в `main`.
Предыдущий спринт (plain Spring + Maven): история в `main`.
