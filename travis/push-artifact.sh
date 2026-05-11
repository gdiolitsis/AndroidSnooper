#!/bin/bash

set -e

if [ "${TRAVIS_PULL_REQUEST}" != "false" ]; then

    echo "Pull request detected. Skipping test artifact deployment."

    exit 0
fi

if [ ! -d "Snooper/build/spoon" ]; then

    echo "Directory Snooper/build/spoon not found."

    exit 1
fi

zip -r reports.zip Snooper/build/spoon

curl -X POST \
    https://content.dropboxapi.com/2/files/upload \
    --header "Authorization: Bearer ${DROPBOX_ACCESS_TOKEN}" \
    --header "Dropbox-API-Arg: {\"path\": \"/builds/reports_${TRAVIS_BUILD_NUMBER}.zip\",\"mode\": \"overwrite\",\"autorename\": true,\"mute\": false}" \
    --header "Content-Type: application/octet-stream" \
    --data-binary @reports.zip
