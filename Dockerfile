FROM maven:3.3-jdk-8

RUN mkdir -p /usr/src/app/
WORKDIR /usr/src/app

ADD . /usr/src/app/
RUN mvn clean && mvn package

ENTRYPOINT ["java","-jar","/usr/src/app/target/rendernode-3.1.0-SNAPSHOT-jar-with-dependencies.jar","--job-path","/usr/local/rendernode/rs_jobs"]
