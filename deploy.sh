#!/bin/bash

echo "TRAVIS_BRANCH=$TRAVIS_BRANCH"
echo "TRAVIS_COMMIT=$TRAVIS_COMMIT"
if [ "$TRAVIS_BRANCH" == "master" ]; then
  mkdir target/deploy
  cp target/kribi.jar target/deploy/.
fi
