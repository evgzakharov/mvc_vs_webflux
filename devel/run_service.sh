#!/bin/sh -ex

sudo docker kill moderation-service | echo 'not found'

sudo docker run -v "$(pwd)/service.jar:/service.jar" \
    -d --rm -p 8080:8080 \
    --name moderation-service \
    openjdk:17 java -jar /service.jar
