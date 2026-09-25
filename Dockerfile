FROM eclipse-temurin:21-jre-alpine

ARG ACTIVE_PROFILE=default
ENV SPRING_PROFILES_ACTIVE=${ACTIVE_PROFILE}

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
