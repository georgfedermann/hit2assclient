#!/usr/bin/env bash

TOOLS_TARGET_PATH=~/bin/
TOOLS_JAR_NAME=HitAssClientTools-jar-with-dependencies.jar
TARGET_FOLDER=target/

echo "Building HitAssClient tools package"
mvn clean package assembly:single && cp ${TARGET_FOLDER}${TOOLS_JAR_NAME} ${TOOLS_TARGET_PATH}
echo "copying current hitAssClientTools.sh to ${TOOLS_TARGET_PATH}"
cp src/main/java/org/poormanscastle/products/hit2assclient/cli/hitAssClientTools.sh ${TOOLS_TARGET_PATH}
echo "copying new ${TOOLS_JAR_NAME} to ${TOOLS_TARGET_PATH}"
cp target/HitAssClientTools-jar-with-dependencies.jar ${TOOLS_TARGET_PATH}

