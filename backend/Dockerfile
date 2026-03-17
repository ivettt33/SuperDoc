# Multi-stage Dockerfile for Spring Boot application
# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install necessary tools
RUN apk add --no-cache bash curl

# Copy Gradle wrapper files (including the jar)
COPY gradle ./gradle
COPY gradlew ./
COPY gradlew.bat ./

# Make gradlew executable
RUN chmod +x ./gradlew

# Copy Gradle configuration files
COPY build.gradle settings.gradle ./

# Copy source code
COPY src ./src

# Build the application (skip tests as they're already run in CI)
# Set environment variables for better network handling and longer timeouts
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.workers.max=2 -Xmx2g -Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=120000"
# Pre-download Gradle distribution to avoid timeout issues
RUN ./gradlew --version || echo "Gradle version check completed"
# Build with retries and better error handling
RUN ./gradlew clean bootJar -x test --no-daemon || \
    (echo "Build failed, retrying in 15 seconds..." && sleep 15 && \
     ./gradlew clean bootJar -x test --no-daemon || \
     (echo "Second retry failed, final attempt in 30 seconds..." && sleep 30 && \
      ./gradlew clean bootJar -x test --no-daemon))

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose the application port
EXPOSE 8080

# Health check (optional - remove if actuator is not configured)
# HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
#   CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

