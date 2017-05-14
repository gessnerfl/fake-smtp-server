#!/bin/bash

cp build/libs/fake-smtp-server-$TRAVIS_TAG.jar build/libs/fake-smtp-server.jar

docker login -e $DOCKER_EMAIL -u $DOCKER_USER -p $DOCKER_PASS

export REPO=gessnerfl/fake-smtp-server

docker build -f Dockerfile -t $REPO:$TRAVIS_TAG .
docker tag $REPO:$TRAVIS_TAG $REPO:latest
docker push $REPO