# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy build files and Gradle wrapper
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

# Normalize line endings and ensure gradlew is executable
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Copy source code
COPY src ./src

# Build production jar skipping tests (as DB is not present during build stage)
RUN ./gradlew bootJar --no-daemon -x test

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S hospital && adduser -S hospital -G hospital
COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar
RUN mkdir -p /app/data && chown -R hospital:hospital /app

USER hospital
EXPOSE 8085

# Cold-start and memory-optimized JVM flags for container environments
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:TieredStopAtLevel=1 -jar app.jar --server.port=${PORT:-8085}"]
