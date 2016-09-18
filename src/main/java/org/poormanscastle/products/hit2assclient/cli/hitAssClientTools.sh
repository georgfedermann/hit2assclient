#!/usr/bin/env bash

JAR_HOME=~/bin
JAR_NAME=HitAssClientTools-jar-with-dependencies.jar

HIT_CLOU_LIBRARY_PATH=`pwd`
# Nota bene: the rather unusual naming of the encoding is not a typo, but rather how javacc expects it!
HIT_CLOU_ENCODING=ISO8859_1

java -Dhit2ass.clou.encoding=$HIT_CLOU_ENCODING -Dhit2ass.clou.path=$HIT_CLOU_LIBRARY_PATH -Dfile.encoding=ISO-8859-1 -cp "${JAR_HOME}${JAR_NAME}" org.poormanscastle.products.hit2assclient.cli.HitAssClientTools
