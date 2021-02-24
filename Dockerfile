FROM adoptopenjdk:15 AS BUILD
WORKDIR /build
COPY . /build
RUN --mount=type=cache,target=/root/.gradle ./gradlew build -x test

FROM nvcr.io/nvidia/cuda:10.1-cudnn7-devel-ubuntu18.04 AS DEPLOY
WORKDIR /app
COPY --from=BUILD /opt/java/openjdk /opt/java/openjdk
COPY --from=BUILD /build/build/libs/kotlin-tensorflow-segmentation-poc.jar /app/app.jar
ENV _JAVA_OPTIONS "-Xmx2g"
CMD /opt/java/openjdk/bin/java -jar /app/app.jar
