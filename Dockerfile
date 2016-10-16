FROM maven:3.3-jdk-8-onbuild
ENV RS_PROCESS_COUNT 1
ENV RS_THREAD_COUNT 2
ENV RS_CHUNKY_XMS 2048
ENV RS_CHUNKY_XMX 2048
CMD exec java -jar /usr/src/app/target/rendernode-3.0.0-SNAPSHOT-jar-with-dependencies.jar \
         -p $RS_PROCESS_COUNT -t $RS_THREAD_COUNT --chunky-xms $RS_CHUNKY_XMS --chunky-xmx $RS_CHUNKY_XMX \
         --job-path /usr/src/app/rs_jobs