#!/bin/sh -ex

sudo docker kill mongo | echo 'not found'

sudo docker run --rm -d -p 27017:27017 mongo:4.4.8
