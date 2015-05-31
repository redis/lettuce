#!/usr/bin/env bash
VERSION=$(xpath pom.xml "/project/version" 2>/dev/null | sed 's/<version>//g;s/<\/version>//g')

BASE=target/redis.paluch.biz
TARGET_BASE=/docs/api/releases/

git clone https://github.com/mp911de/redis.paluch.biz.git target/redis.paluch.biz
cd target/redis.paluch.biz && git checkout gh-pages && cd ../..

if [[ ${VERSION} == *"SNAPSHOT"* ]]
then
    TARGET_BASE=/docs/api/snapshots/
fi

TARGET_DIR=${BASE}${TARGET_BASE}${VERSION}

mkdir -p ${TARGET_DIR}
mkdir -p ${BASE}${TARGET_BASE}latest

cp -R target/site/apidocs/* ${TARGET_DIR}
cp -R target/site/apidocs/* ${BASE}${TARGET_BASE}latest


git add .
git commit -m "Generated API docs" && git push



