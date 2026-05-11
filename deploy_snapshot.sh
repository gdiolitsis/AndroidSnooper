#!/bin/bash

set -e

if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then

    echo "Pull request detected. Skipping snapshot deployment."

    exit 0
fi

echo "Deploying snapshot build..."

chmod +x ./gradlew

./gradlew publishToMavenLocal

echo "Snapshot deployment completed."
