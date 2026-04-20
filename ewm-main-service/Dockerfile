FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY stats/pom.xml stats/pom.xml
COPY stats/stats-dto/pom.xml stats/stats-dto/pom.xml
COPY stats/stats-client/pom.xml stats/stats-client/pom.xml
COPY ewm-main-service/pom.xml ewm-main-service/pom.xml

COPY stats/stats-dto/src stats/stats-dto/src
COPY stats/stats-client/src stats/stats-client/src
COPY ewm-main-service/src ewm-main-service/src

RUN mvn -pl ewm-main-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/ewm-main-service/target/ewm-main-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
