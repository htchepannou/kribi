#!/bin/bash

echo "TRAVIS_BRANCH=$TRAVIS_BRANCH"
if [ "$TRAVIS_BRANCH" == "master" ]; then
  curl -X POST -d @target/kribi.jar -H "api_key: $KRIBI_API_KEY" http://kribi.tchepannou.io/artifact/kribi/$TRAVIS_PULL_REQUEST_SHA
fi
