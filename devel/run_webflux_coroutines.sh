#!/bin/sh -ex

./gradlew -x test :webflux_coroutines:build

docker run -v "$(PWD)/webflux_coroutines/build/libs/webflux_coroutines.jar:/webflux_coroutines.jar" \
    -m 1024m --memory-swap 1024m --cpus 4  \
    --name webflux-coroutine-test \
    -it --rm -p 8080:8080 \
    openjdk:17 java -jar /webflux_coroutines.jar