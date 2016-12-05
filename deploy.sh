#!/bin/bash

echo "TRAVIS_BRANCH=$TRAVIS_BRANCH"
echo "TRAVIS_COMMIT=$TRAVIS_COMMIT"
if [ "$TRAVIS_BRANCH" == "master" ]; then
  curl -X POST -F "file=@target/kribi.jar"  -H "api_key: $KRIBI_API_KEY" http://kribi.tchepannou.io/v1/application/artifact/kribi/$TRAVIS_COMMIT
fi
