# My Blog Backend App

Backend for the [my-blog-front-app](../my-blog-front-app) frontend (blog лента постов + страница поста).

- **Java 21**, **Spring Framework 6.1** (без Spring Boot), сборка **Maven**, упаковка **WAR**
- Сервлет-контейнер: **Tomcat 10.1** (подойдёт и Jetty 12, см. деплой)
- БД: **H2 in-memory** (схема создаётся при старте из `schema.sql`, демо-данные — из `data.sql`)
- Слои: `controller` → `service` → `dao` (JdbcTemplate) → H2; DTO для REST-контракта; `GlobalExceptionHandler` (404/400 в JSON)
- Тесты: JUnit 5 + Spring TestContext (кеширование контекстов), Mockito, MockMvc, AssertJ

## Быстрый старт

Требования: JDK 21+, Maven 3.9+, Docker (опционально), Tomcat 10.1 (для ручного деплоя).

```sh
git clone <your-fork-url> my-blog-back-app
cd my-blog-back-app
git checkout develop

# тесты (20 тестов, всё зелёное)
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

- `dao/*Test` — DAO на встроенной H2 (общий кешированный контекст `TestConfig`)
- `service/*Test` — сервисы + реальный DAO-слой на H2 (тот же контекст)
- `controller/PostControllerMvcTest` — MVC-срез: контроллеры + Jackson + handler, сервисы замоканы (без контекста)
- `BlogIntegrationTest` — сквозной сценарий по HTTP (MockMvc + сервисы + H2, контекст `WebTestConfig`)

Контекстов всего два (`TestConfig`, `WebTestConfig`), оба переиспользуются — кеширование Spring TestContext.

## Структура

```
src/main/java/com/example/blog/
  config/AppConfig.java               # MVC, CORS, DataSource(H2), Tx, Multipart, init schema.sql+data.sql
  config/WebAppInitializer.java       # bootstrap DispatcherServlet -> "/"
  controller/PostController.java      # /api/posts + likes + image
  controller/CommentController.java   # /api/posts/{postId}/comments
  service/PostService(Impl).java      # пагинация, превью 128 символов + "…", валидация
  service/CommentService(Impl).java
  dao/PostDao(Impl).java             # JdbcTemplate: посты, теги, картинка, поиск по title/text/tag
  dao/CommentDao(Impl).java
  model/Post.java, Comment.java, ImageData.java
  dto/PostDto.java, PostListResponse.java, Create/UpdatePostRequest.java,
      CommentDto.java, Create/UpdateCommentRequest.java
  exception/NotFoundException.java, GlobalExceptionHandler.java
src/main/resources/schema.sql         # posts, post_tags, comments (CASCADE)
src/main/resources/data.sql           # демо-данные для ручной проверки с фронтом
```

Схема БД: `posts(id, title, text, likes_count, image, image_content_type)`,
`post_tags(post_id → posts ON DELETE CASCADE, tag)`,
`comments(id, post_id → posts ON DELETE CASCADE, text)`.

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

Ошибки: `404 {"error": "..."}` (нет поста/комментария/картинки), `400 {"error": "..."}` (пустые title/text).
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
