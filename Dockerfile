# Use official OpenJDK runtime as base image
FROM amazoncorretto:25-alpine-jdk

# Set working directory
WORKDIR /app

# Copy the JAR file into the container
COPY build/libs/evaluation-service-1.0.0.jar app.jar

# Expose port 8080
EXPOSE 8080

# Set JVM options for optimal performance
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]