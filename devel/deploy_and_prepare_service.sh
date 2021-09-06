#!/bin/sh -ex

host=nerumb@192.168.1.74

./gradlew :service:build

scp $(PWD)/service/build/libs/service.jar $host:service.jar

scp $(PWD)/devel/run_service.sh $host:run_service.sh
scp $(PWD)/devel/run_mongo.sh $host:run_mongo.sh