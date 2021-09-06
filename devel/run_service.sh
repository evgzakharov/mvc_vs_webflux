#!/bin/sh -ex

sudo docker kill moderation-service | echo 'not found'

sudo docker run -v "$(pwd)/service.jar:/service.jar" \
    -d --rm -p 8080:8080 \
    --name moderation-service \
    adoptopenjdk/openjdk16:jre-16.0.1_9-alpine java -jar /service.jar
