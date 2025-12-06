FROM gradle:jdk25-corretto AS build
WORKDIR /app
ADD . /app
RUN chmod 755 gradlew
RUN  ./gradlew dependencies
RUN ./gradlew bootJar
COPY build/libs/prontuario*-SNAPSHOT.jar prontuario-0.0.1-SNAPSHOT.jar


FROM amazoncorretto:25-alpine3.22
WORKDIR /app
COPY --from=build build/libs/prontuario*-SNAPSHOT.jar app.jar
COPY --from=build src/main/resources/application.properties application.properties
COPY --from=build src/main/resources/application-prod.properties application-prod.properties
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
