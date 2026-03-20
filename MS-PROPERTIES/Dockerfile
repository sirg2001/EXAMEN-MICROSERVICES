# Stage 1: Build
FROM maven:3.8-eclipse-temurin-11 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -Dmaven.wagon.http.retryHandler.count=3 || mvn dependency:go-offline -Dmaven.wagon.http.retryHandler.count=3

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:11-jre
WORKDIR /app

# Créer un utilisateur non-root pour la sécurité
RUN useradd -m appuser

COPY --from=builder /build/target/*.jar app.jar
COPY --from=builder /build/pom.xml .

# Donner les permissions
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8081

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8081/api -o /dev/null -s || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
