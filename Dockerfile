# syntax=docker/dockerfile:experimental

FROM maven:3-jdk-13-alpine AS BUILD
WORKDIR /build
COPY . /build
ENV MAVEN_OPTS "-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=WARN -Dorg.slf4j.simpleLogger.showDateTime=true -Djava.awt.headless=true"
ENV MAVEN_CLI_OPTS "--batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true -DdeployAtEnd=true"
RUN --mount=type=cache,target=/root/.m2/repository mvn $MAVEN_CLI_OPTS clean package

FROM openjdk:14-jdk AS DEPLOY
WORKDIR /app
COPY --from=BUILD /build/target/*-cpu.jar /app/app.jar
ENV _JAVA_OPTIONS "-Xmx64m -Xms64m -Djava.security.file=file:/dev/./urandom -XX:+UnlockExperimentalVMOptions -XX:+UseZGC"
CMD java -jar /app/app.jar
