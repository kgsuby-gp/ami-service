# Use a lightweight OpenJDK image
FROM eclipse-temurin:17-jdk-alpine
# Copy the jar file from the target folder
COPY target/*.jar app.jar
# Command to run the application
ENTRYPOINT ["java","-jar","/app.jar"]
