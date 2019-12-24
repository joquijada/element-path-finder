#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_6d657824463d_key -iv $encrypted_6d657824463d_iv -in cd/codesigning.asc.enc -out cd/codesigning.asc -d
    gpg --import --batch cd/codesigning.asc
fi
