#!/usr/bin/env bash

JAR_HOME=~/bin
JAR_NAME=HitAssClientTools-jar-with-dependencies.jar

HIT_CLOU_LIBRARY_PATH=`pwd`
# Nota bene: the rather unusual naming of the encoding is not a typo, but rather how javacc expects it!
HIT_CLOU_ENCODING=ISO8859_1

# the path to and the file name of the workspace containing the Hit2Ass deployed module library
HIT2ASS_DEPLOYED_MODULE_LIBRARY=$(pwd)/Hit2AssDeployedModuleLibrary.acr

echo "Using: "
echo "JAR_HOME=$JAR_HOME"
echo "JAR_NAME=$JAR_NAME"
echo "HIT_CLOU_LIBRARY_PATH=$HIT_CLOU_LIBRARY_PATH"
echo "HIT_CLOU_ENCODING=$HIT_CLOU_ENCODING"

echo "Deleting old build artifacts:"
if [ -e ${HIT2ASS_DEPLOYED_MODULE_LIBRARY} ]
then
  echo "${HIT2ASS_DEPLOYED_MODULE_LIBRARY} found and will be deleted."
  rm ${HIT2ASS_DEPLOYED_MODULE_LIBRARY}
fi
if [ -e hitass.log ]
then
  echo "hitass.log found and will be deleted."
  rm hitass.log
fi

java -Dhit2ass.clou.pathToDeployedModuleLibrary=${HIT2ASS_DEPLOYED_MODULE_LIBRARY} -Dhit2ass.clou.encoding=$HIT_CLOU_ENCODING -Dhit2ass.clou.path=$HIT_CLOU_LIBRARY_PATH -Dfile.encoding=ISO-8859-1 -cp "${JAR_HOME}/${JAR_NAME}" org.poormanscastle.products.hit2assclient.cli.HitAssClientTools
