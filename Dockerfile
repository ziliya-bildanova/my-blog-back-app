# Build executable jar with Gradle, run on JRE 21 (embedded Tomcat inside the jar).
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app
COPY settings.gradle build.gradle ./
COPY src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/my-blog-back-app-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
