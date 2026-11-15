# =====================================================
# Stage 1: Build the Spring Boot application (Java 21)
# =====================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first to leverage Docker layer caching
COPY pom.xml .

# Download Maven dependencies
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Compile and package application (Java 21)
RUN mvn clean package -DskipTests

# =====================================================
# Stage 2: Create lightweight runtime image (Java 21)
# =====================================================
FROM eclipse-temurin:21-jre

# Install curl for container health checks
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
