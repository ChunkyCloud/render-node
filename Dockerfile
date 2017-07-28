FROM openjdk:8-jre-alpine

RUN mkdir -p /usr/local/rendernode
WORKDIR /usr/local/rendernode

COPY ./target/rendernode-*-jar-with-dependencies.jar /usr/local/rendernode/rendernode.jar

ENTRYPOINT ["java","-jar","/usr/local/rendernode/rendernode.jar","--job-path","/usr/local/rendernode/rs_jobs"]
