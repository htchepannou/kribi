#!/bin/bash

if [ "$TRAVIS_BRANCH" == "master" ]; then
  mkdir target/deploy
  cp target/kribi.jar target/deploy/.
fi
