FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

RUN ./gradlew dependencies --no-daemon || true

COPY src src

RUN ./gradlew buildFatJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/*-all.jar app.jar
COPY --from=build /app/src/main/resources/db/migration /app/db/migration

ENV PORT=8080
ENV FLYWAY_FILESYSTEM_LOCATION=/app/db/migration
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
