FROM openjdk:17.0.1-jdk-slim
WORKDIR /app
COPY build/libs/stock-automation-1.0.0.jar /app/application.jar
COPY src/main/resources/templates /app/templates
COPY src/main/resources/static /app/static
ENTRYPOINT ["java", "-jar", "application.jar"]
EXPOSE 8085