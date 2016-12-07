#!/bin/bash

if [ "$TRAVIS_BRANCH" == "master" ]; then
  wget http://kribi.tchepannou.io/v1/application/kribi/init_artifact?version=$TRAVIS_COMMIT

  wget http://kribi.tchepannou.io/v1/application/kribi/deploy?name=kribi&environment=PROD&region=us-east-1&undeployOld=true&version=$TRAVIS_COMMIT
  exit $?

fi
