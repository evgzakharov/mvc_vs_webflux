#!/bin/sh -ex

./gradlew -x test :webflux:build

docker run -v "$(pwd)/webflux/build/libs/webflux.jar:/webflux.jar" \
    -m 512m --memory-swap 512m --cpus 0.5 \
    --name webflux-test \
    -it --rm -p 8080:8080 \
    openjdk:17 java -jar /webflux.jar