#!/bin/sh -ex

./gradlew -x test :mvc:build

docker run -v "$(PWD)/mvc/build/libs/mvc.jar:/mvc.jar" \
    -m 1024m --memory-swap 1024m --cpus 4  \
     --name mvc-test \
    -it --rm -p 8080:8080 \
    openjdk:17 java -jar /mvc.jar