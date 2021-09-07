#!/bin/sh -ex

./gradlew :mvc:build

docker run -v "$(PWD)/mvc/build/libs/mvc.jar:/mvc.jar" \
    -m 1GB --memory-swap 1GB --cpus 4  \
    -it --rm -p 8080:8080 \
    adoptopenjdk/openjdk16:jre-16.0.1_9-alpine java -jar /mvc.jar