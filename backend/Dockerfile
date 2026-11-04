# Multi-stage Dockerfile for Collobster Spring Boot Backend
# Stage 1: Build JAR using official Maven image (bypasses mvnw wget 429 rate limit)
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace
COPY settings.xml ./
COPY pom.xml ./
COPY src/ src/
RUN mvn clean package -DskipTests -B -s settings.xml

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S collabster && adduser -S collabster -G collabster
COPY --from=builder /workspace/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R collabster:collabster /app
USER collabster

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/api/v1/system/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseSerialGC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
