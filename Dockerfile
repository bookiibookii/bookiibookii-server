FROM eclipse-temurin:17-jre

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} bookiiV1.jar

ENTRYPOINT ["java", "-jar", "/app/bookiiV1.jar"]