# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

#Copy all files
COPY . .

#Make Gradle executable
RUN chmod +x main/gradlew

#Build jar
RUN ./main/gradlew -p main :backend:bootJar -x test

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

#Copy jar in new container
COPY --from=build /app/main/backend/build/libs/*.jar app.jar

EXPOSE 8080

# Starte die Spring Boot Anwendung
ENTRYPOINT ["java", "-jar", "app.jar"]
