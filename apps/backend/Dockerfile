# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper and pom first to cache dependencies
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Make Maven wrapper executable and download dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:resolve --no-transfer-progress

# Copy source code and build the application (skip tests for faster builds)
COPY src ./src
RUN ./mvnw package -DskipTests --no-transfer-progress \
    && f="$(find target -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -print -quit)" \
    && test -n "$f" && cp "$f" /app/app.jar

# ---- Stage 2: Runtime ----
FROM gcr.io/distroless/java21-debian12 AS runtime

WORKDIR /app

COPY --from=build /app/app.jar app.jar

# Run as the built-in non-root user (uid 65532)
USER nonroot

# Expose the default Spring Boot port
EXPOSE 8080

# Run with container-aware JVM settings
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
