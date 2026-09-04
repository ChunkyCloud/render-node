FROM eclipse-temurin:25 AS build
WORKDIR /usr/src/app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:25
RUN mkdir -p /opt/cc-rendernode/data
COPY --from=build /usr/src/app/build/libs/rendernode.jar /opt/cc-rendernode/rendernode.jar
WORKDIR /opt/cc-rendernode/data
ENTRYPOINT ["java", "-jar", "/opt/cc-rendernode/rendernode.jar"]
