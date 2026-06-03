#!/bin/bash
set -euo pipefail

VERSION=${1}
GIT_TAG=v${VERSION}
# Portable BASEDIR resolution (BSD readlink has no -f).
BASEDIR="$(cd "$(dirname "$0")/.." && pwd)"
(
    cd "$BASEDIR"
    # Use perl for in-place edits: identical behaviour on BSD (macOS) and GNU (Linux/CI),
    # unlike `sed -i` whose syntax differs between the two.
    perl -i -pe "s/^version=.*/version=${VERSION}/" gradle.properties
    perl -i -pe "s/^## unreleased\$/## unreleased\n## ${GIT_TAG}/" CHANGELOG.md
    perl -i -pe "s/val flutterKmpVersion = .*/val flutterKmpVersion = \"${VERSION}\"/" example/build.gradle.kts
    git commit -m "version ${VERSION}" gradle.properties CHANGELOG.md example/build.gradle.kts
    git tag -a "$GIT_TAG" -m "version ${VERSION}"
)
