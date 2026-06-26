# =====================================================
# Stage 1: Build the Spring Boot application
# =====================================================

# Use Maven with Java 21 to compile the project
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy only the pom.xml first to leverage Docker layer caching.
# Dependencies will only be downloaded again if pom.xml changes.
COPY pom.xml .

# Download all Maven dependencies
RUN mvn dependency:go-offline -B

# Copy the application source code
COPY src ./src

# Build the application and generate the executable JAR
# Tests are skipped to reduce build time in CI/CD.
RUN mvn clean package -DskipTests


# =====================================================
# Stage 2: Create a lightweight runtime image
# =====================================================

# Use a smaller JRE image since compilation is already complete
FROM eclipse-temurin:21-jre

# Set the application directory
WORKDIR /app

# Copy the generated JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Declare a volume for persistent file storage
# This directory can be mounted from the host machine.
VOLUME /storage

# Expose the Spring Boot application port
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
