#!/bin/bash

chmod +x gradlew

if [ "$TRAVIS_PULL_REQUEST" == "true" ]; then
  echo -e "Build Pull Request #$TRAVIS_PULL_REQUEST => Branch [$TRAVIS_BRANCH]"
  ./gradlew build sonarqube
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" == "" ] && [ "$TRAVIS_BRANCH" != "master" ] ; then
  echo -e 'Build Feature Branch ['$TRAVIS_BRANCH']'
  ./gradlew build
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" == "" ] ; then
  echo -e 'Build Branch ['$TRAVIS_BRANCH']'
  ./gradlew build sonarqube
elif [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" != "" ]; then
  echo -e 'Build Tag ['$TRAVIS_TAG']'
  ./gradlew build sonarqube
else
  echo -e 'WARN: Should not be here => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']  Pull Request ['$TRAVIS_PULL_REQUEST']'
  ./gradlew build
fi