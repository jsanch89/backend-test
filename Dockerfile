# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY domain ./domain
COPY application ./application
COPY infrastructure ./infrastructure

RUN chmod +x gradlew && ./gradlew --no-daemon :infrastructure:bootJar

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=build /workspace/infrastructure/build/libs/*.jar app.jar

EXPOSE 5001
ENV EXISTING_APIS_BASE_URL=http://host.docker.internal:3001

ENTRYPOINT ["java", "-jar", "app.jar"]
