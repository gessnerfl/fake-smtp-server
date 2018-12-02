#!/bin/bash

echo "Copy deployment artifact"
cp build/libs/fake-smtp-server-$TRAVIS_TAG.jar build/libs/fake-smtp-server.jar

echo "Check docker version"
docker --version

echo "Login to dockerhub"
docker login -u $DOCKER_USER -p $DOCKER_PASS

export REPO=gessnerfl/fake-smtp-server

echo "Build docker image"
docker build -f Dockerfile -t $REPO:$TRAVIS_TAG .
echo "Tag docker images as latest"
docker tag $REPO:$TRAVIS_TAG $REPO:latest
echo "Push to dockerhub"
docker push $REPO:$TRAVIS_TAG
docker push $REPO:latest