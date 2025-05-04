# Stage 1: Build the application using Gradle
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Copy Gradle project files
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Pre-download dependencies
RUN ./gradlew build --no-daemon || return 0

# Copy the rest of the source code
COPY src ./src

# Build the JAR
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run the application with a slim JDK image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the previous stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]