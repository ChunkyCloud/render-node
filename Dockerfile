FROM maven:3.8-jdk-8

RUN mkdir -p /usr/src/app/
WORKDIR /usr/src/app

ADD . /usr/src/app/
RUN mvn clean && mvn package

# cache directory for player skins; TODO customize the path in the future
RUN mkdir -p /root/.chunky/cache

ENTRYPOINT ["java","-jar","/usr/src/app/target/rendernode-1.4.1-jar-with-dependencies.jar","--job-path","/usr/local/rendernode/rs_jobs","--texturepacks-path","/usr/src/app/texturepacks"]
