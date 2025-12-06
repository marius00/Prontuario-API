FROM gradle:jdk21-corretto AS build
WORKDIR /app
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY gradle/wrapper gradle/wrapper
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts
COPY .gradle .gradle
RUN ./gradlew dependencies

ADD . /app
RUN ./gradlew --no-watch-fs dependencies # Dependencies
RUN ./gradlew --no-watch-fs generateJava # Schema
RUN ./gradlew --no-watch-fs bootJar
COPY build/libs/prontuario*-SNAPSHOT.jar prontuario-0.0.1-SNAPSHOT.jar
COPY src/main/resources/application.properties application.properties
COPY src/main/resources/application-prod.properties application-prod.properties


FROM amazoncorretto:21-alpine3.22
WORKDIR /app
COPY --from=build /app/prontuario-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/application.properties application.properties
COPY src/main/resources/application-prod.properties application-prod.properties
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
