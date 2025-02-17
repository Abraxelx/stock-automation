# Build stage
FROM gradle:8.0.2-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/stock-automation-*.jar app.jar

# Metadata
LABEL maintainer="Halil Sahin"
LABEL version="2.0.0"
LABEL description="Stock Automation Application"

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/ || exit 1

# Container config
EXPOSE 8080
ENV TZ=Europe/Istanbul
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
