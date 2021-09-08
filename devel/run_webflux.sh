#!/bin/sh -ex

./gradlew :webflux:build

docker run -v "$(PWD)/webflux/build/libs/webflux.jar:/webflux.jar" \
    -m 512m --memory-swap 512m --cpus 2  \
    -it --rm -p 8080:8080 \
    adoptopenjdk/openjdk16:jre-16.0.1_9-alpine java -jar /webflux.jar