# syntax=docker/dockerfile:experimental

FROM adoptopenjdk:15 AS BUILD
WORKDIR /build
COPY . /build
RUN --mount=type=cache,target=/root/.gradle ./gradlew build -x test

FROM adoptopenjdk:15 AS DEPLOY
WORKDIR /app
COPY --from=BUILD /build/target/*-cpu.jar /app/app.jar
ENV _JAVA_OPTIONS "-Xmx64m -Xms64m -Djava.security.file=file:/dev/./urandom -XX:+UseZGC"
CMD java -jar /app/app.jar
