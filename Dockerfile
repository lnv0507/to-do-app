# ============================================================
# Stage 1 – Build
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper & dependency manifests first (layer cache)
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/ gradle/

# Download dependencies (cached as long as build.gradle doesn't change)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q || true

# Copy source and build the WAR (skip tests during image build)
COPY src/ src/

RUN ./gradlew bootWar -x test --no-daemon

# ============================================================
# Stage 2 – Run
# ============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the executable WAR produced by Spring Boot's bootWar task
COPY --from=builder /app/build/libs/*.war app.war

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.war"]
