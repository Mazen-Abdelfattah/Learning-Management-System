# ─────────────────────────────────────────────
# Stage 1: Build
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (better layer caching)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -q

# Copy source and build the JAR
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ─────────────────────────────────────────────
# Stage 2: Runtime
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the final JAR from the builder stage
COPY --from=builder /app/target/LMS-0.0.1-SNAPSHOT.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]