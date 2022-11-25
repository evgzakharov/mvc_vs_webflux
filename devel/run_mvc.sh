#!/bin/sh -ex

./gradlew -x test :mvc:build

docker run -v "$(pwd)/mvc/build/libs/mvc.jar:/mvc.jar" \
    -m 512m --memory-swap 512m --cpus 0.5  \
     --name mvc-test \
    -it --rm -p 8080:8080 \
    openjdk:17 java -jar /mvc.jar