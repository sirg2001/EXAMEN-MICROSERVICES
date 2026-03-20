# ========================================
# Stage 1: BUILD - Compile and Package
# ========================================
FROM maven:3.8-eclipse-temurin-11 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -Dmaven.wagon.http.retryHandler.count=3 || mvn dependency:go-offline -Dmaven.wagon.http.retryHandler.count=3

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# ========================================
# Stage 2: RUNTIME - Run Application
# ========================================
FROM eclipse-temurin:11-jre

LABEL maintainer="rng-realestate@dev.local"
LABEL version="1.0.0"
LABEL description="MS-Payments Microservice - Payment Processing"

# Create app user
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy WAR from builder
COPY --from=builder /build/target/*.jar app.jar

# Create logs directory
RUN mkdir -p logs && chown -R appuser:appuser /app

# Change to app user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -version || exit 1

# Expose port
EXPOSE 8083

# JVM options for optimized performance
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
