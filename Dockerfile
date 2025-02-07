FROM gradle:8.0.2-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build --no-daemon

FROM openjdk:17.0.1-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/stock-automation-*.jar /app/application.jar
COPY src/main/resources/templates /app/templates
COPY src/main/resources/static /app/static
ENTRYPOINT ["java", "-jar", "application.jar"]
EXPOSE 8080