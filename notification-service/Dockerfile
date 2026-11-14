# ---- sources -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS base
WORKDIR /app
ENV MAVEN_OPTS=-Xmx512m
COPY pom.xml .
COPY src src

# ---- unit tests (scripts/test.sh notification-service) --------------------
FROM base AS test
ARG MVN_ARGS=""
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp test ${MVN_ARGS}

# ---- formatting (scripts/format.sh) --------------------------------------
FROM base AS format
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q spotless:apply
FROM scratch AS format-export
COPY --from=format /app/src /

# ---- package (tests are not run here: a failing test must not break `docker compose up`)
FROM base AS build
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

# ---- runtime -------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
