FROM gradle:jdk21-corretto AS build
WORKDIR /app

# Download gradle before we load our dependencies
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY gradle/wrapper gradle/wrapper
RUN chmod 755 gradlew
RUN ./gradlew --version

# Image layer keeps dependencies cached
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts
RUN ./gradlew dependencies

# Copy source and build the application
ADD . /app
RUN chmod 755 gradlew
RUN ./gradlew --no-watch-fs bootJar
RUN mv build/libs/prontuario-0.0.1-SNAPSHOT.jar prontuario-0.0.1-SNAPSHOT.jar


# Final runtime image without JDK
FROM amazoncorretto:21-alpine3.22
WORKDIR /app
COPY --from=build /app/prontuario-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/application.properties application.properties
COPY src/main/resources/application-prod.properties application-prod.properties
ENTRYPOINT ["java", "-jar", "app.jar"]
