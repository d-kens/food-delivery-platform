# syntax=docker/dockerfile:1

# Two-stage build for user-service:
#   1. Build stage  - compiles the Spring Boot jar with Gradle (JDK) and splits it into layers.
#   2. Runtime stage - copies only the layers onto a slim JRE image and runs as a non-root user.
# Gradle caches persist between builds, and dependency layers are reused when only code changes.

# ---- Build stage ----
FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

# Gradle wrapper, build config and source code
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

# Build the jar, keeping Gradle's caches between builds
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test

# Split the jar into layers so code changes don't rewrite the dependency layer
RUN cp build/libs/*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --destination extracted


# ---- Runtime stage ----
FROM eclipse-temurin:25-jre

# Unprivileged user to run the app
RUN groupadd --system app && useradd --system --gid app app

WORKDIR /app

# Least to most frequently changing, so earlier layers stay cached
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
