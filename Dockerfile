# https://hub.docker.com/_/eclipse-temurin/tags
FROM eclipse-temurin:25.0.3_9-jdk-noble

COPY  . /project
WORKDIR /project

RUN chmod +x ./gradlew

RUN mkdir /root/.gradle
