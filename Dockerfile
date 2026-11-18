FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml pom.xml
RUN mvn -B -ntp dependency:go-offline
COPY backend/src src
RUN mvn -B -ntp package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system learning && useradd --system --gid learning learning
COPY --from=build --chown=learning:learning /build/target/learning-score-1.0.0.jar app.jar
USER learning
ENV SPRING_PROFILES_ACTIVE=railway
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "/app/app.jar"]
