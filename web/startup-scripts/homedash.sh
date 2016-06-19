#!/bin/bash

cd cfg

# main class name
BIN_NAME="com.ftpix.homedash.app.App"

LIB_PATH=../lib
BIN_PATH=../bin
PLUGIN_PATH=../plugins


PATH=$JAVA_HOME/bin/:$PATH:../bin/:.:

CLASSPATH=$CLASSPATH:../cfg/:
for filename in `ls ../lib|grep jar`
do
        CLASSPATH=$CLASSPATH$LIB_PATH/$filename:
done
for filename in `ls ../bin|grep jar`
do
        CLASSPATH=$CLASSPATH$BIN_PATH/$filename:
done
for filename in `ls ../plugins|grep jar`
do
        CLASSPATH=$CLASSPATH$PLUGIN_PATH/$filename:
done

CLASSPATH=$CLASSPATH.:


echo "****************"
echo $CLASSPATH
echo "****************"


export CLASSPATH

java $BIN_NAME
