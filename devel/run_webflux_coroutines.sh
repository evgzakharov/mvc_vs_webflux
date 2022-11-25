#!/bin/sh -ex

./gradlew -x test :webflux_coroutines:build

docker run -v "$(pwd)/webflux_coroutines/build/libs/webflux_coroutines.jar:/webflux_coroutines.jar" \
    -m 512m --memory-swap 512m --cpus 0.5  \
    --name webflux-coroutine-test \
    -it --rm -p 8080:8080 \
    openjdk:17 java -jar /webflux_coroutines.jar