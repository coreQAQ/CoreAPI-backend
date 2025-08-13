FROM eclipse-temurin:17-jre
WORKDIR /app

COPY target/CoreAPI-main-0.0.1.jar app.jar
COPY application-prod.yml .

ENTRYPOINT ["sh","-c","java -jar app.jar --spring.profiles.active=prod"]