#!/bin/sh -ex

./gradlew -x test :mvc_undertow:build

docker run -v "$(pwd)/mvc_undertow/build/libs/mvc_undertow.jar:/undertow.jar" \
    -m 512m --memory-swap 512m --cpus 0.5  \
     --name undertow-test \
    -it --rm -p 8080:8080 \
    openjdk:17 java -jar /undertow.jar