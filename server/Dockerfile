# Build stage
FROM gradle:8.5-jdk21 AS build

WORKDIR /app

# Copy Gradle files
COPY gradle/ ./gradle/
COPY gradlew ./
COPY gradlew.bat ./
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY gradle.properties ./

# Copy source code
COPY src/ ./src/

# Build the application (Ktor plugin creates shadow JAR)
RUN ./gradlew build --no-daemon -x test

# Find and copy the shadow JAR (server-all.jar or server-*-all.jar)
RUN find /app/build/libs -name "*-all.jar" -exec cp {} /app/server.jar \;

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/server.jar ./server.jar

# Copy application configuration
COPY --from=build /app/src/main/resources/application.yaml ./
COPY --from=build /app/src/main/resources/logback.xml ./

# Expose the application port
EXPOSE 3000

# Run the application
ENTRYPOINT ["java", "-jar", "server.jar"]

