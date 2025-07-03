#!/bin/bash

VERSION=${1}
GIT_TAG=v${VERSION}
BASEDIR=$(dirname $(readlink -f "$0"))
(
    cd "$BASEDIR/.."
    sed -i "s/version=.*/version=${VERSION}/g" gradle.properties
    sed -i "/\#\# unreleased/a \#\# ${GIT_TAG}" CHANGELOG.md
    sed -i "s/val flutterKmpVersion = .*/val flutterKmpVersion = \"${VERSION}\"/g" example/build.gradle.kts
    git commit -m "version ${VERSION}" gradle.properties CHANGELOG.md example/build.gradle.kts
    git tag -a $GIT_TAG -m "version ${VERSION}"
)
