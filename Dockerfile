FROM eclipse-temurin:17-jre

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} bookiiDemo.jar

ENTRYPOINT ["java", "-jar", "/app/bookiiDemo.jar"]