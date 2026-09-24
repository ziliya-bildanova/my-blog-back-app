# Build WAR with Maven, run on Tomcat 10.1 (Jakarta EE, Spring 6.1 compatible).
# Deployed as ROOT so the frontend (http://localhost:8080/api/...) works without a context prefix.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

FROM tomcat:10.1-jdk21
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/target/my-blog-back-app.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
