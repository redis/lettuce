#!/bin/bash
VERSION=$(xpath -e "/project/version" pom.xml 2>/dev/null | sed 's/<version>//g;s/<\/version>//g')

if [[ "" == "${VERSION}" ]]
then
    echo "Cannot determine version"
    exit 1
fi

BASE=target/redis.paluch.biz
TARGET_BASE=/docs/api/releases/

git clone https://github.com/mp911de/redis.paluch.biz.git ${BASE}
cd ${BASE} && git checkout gh-pages && cd ../..

if [[ ${VERSION} == *"SNAPSHOT"* ]]
then
    TARGET_BASE=/docs/api/snapshots/
fi

TARGET_DIR=${BASE}${TARGET_BASE}${VERSION}

mkdir -p ${TARGET_DIR}

cp -R target/site/apidocs/* ${TARGET_DIR}

cd ${BASE}
git add .
git commit -m "Generated API docs"

