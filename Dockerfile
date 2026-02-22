# ─────────────────────────────────────────────
# Stage 1: Build
# Uses an official Maven image — no mvnw needed
# ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml first for better layer caching
# (dependencies are re-downloaded only if pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build the JAR
COPY src ./src
RUN mvn package -DskipTests -q

# ─────────────────────────────────────────────
# Stage 2: Runtime
# Lightweight JRE-only image (no compiler/Maven)
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/LMS-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]