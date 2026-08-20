# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S smartcare && adduser -S smartcare -G smartcare
COPY --from=build /app/build/libs/Hospital_Management-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/data && chown -R smartcare:smartcare /app
USER smartcare
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
# Faster cold start when a free instance wakes from sleep.
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:TieredStopAtLevel=1 -jar app.jar --server.port=${PORT:-8080}"]
