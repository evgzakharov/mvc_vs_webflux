#!/bin/sh -ex

./gradlew -x test :webflux:build

docker run -v "$(PWD)/webflux/build/libs/webflux.jar:/webflux.jar" \
    -m 1024m --memory-swap 1024m --cpus 4  \
    --name webflux-test \
    -it --rm -p 8080:8080 \
    openjdk:17 java -jar /webflux.jar