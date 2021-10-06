#!/bin/sh -ex

./gradlew -x test :webflux_coroutines:build

docker run -v "$(PWD)/webflux_coroutines/build/libs/webflux_coroutines.jar:/webflux_coroutines.jar" \
    -m 256m --memory-swap 256m --cpus 2  \
    -it --rm -p 8080:8080 \
    adoptopenjdk/openjdk16:jre-16.0.1_9-alpine java -jar /webflux_coroutines.jar