FROM arm64v8/amazoncorretto:25-alpine3.22

WORKDIR /app
COPY prontuario-0.0.1-SNAPSHOT.jar /app/prontuario-0.0.1-SNAPSHOT.jar
COPY src/main/resources/application.properties /app/application.properties
COPY src/main/resources/application-prod.properties /app/application-prod.properties
CMD ["java", "-jar", "/app/prontuario-0.0.1-SNAPSHOT.jar"]
