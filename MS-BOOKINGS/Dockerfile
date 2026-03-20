# Stage 1: Build
FROM maven:3.8-eclipse-temurin-11 as builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:11-jre
WORKDIR /app

RUN useradd -m appuser

COPY --from=builder /build/target/*.jar app.jar
COPY --from=builder /build/pom.xml .

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8082

HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8082/api -o /dev/null -s || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
