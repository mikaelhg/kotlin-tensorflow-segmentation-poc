# syntax=docker/dockerfile:experimental

FROM adoptopenjdk:15 AS BUILD
WORKDIR /build
COPY . /build
RUN --mount=type=cache,target=/root/.gradle ./gradlew build -x test

FROM adoptopenjdk:15 AS DEPLOY
WORKDIR /app
COPY --from=BUILD /build/build/libs/kotlin-tensorflow-segmentation-poc.jar /app/app.jar
ENV _JAVA_OPTIONS "-Xmx512m -Xms512m"
CMD java -jar /app/app.jar
