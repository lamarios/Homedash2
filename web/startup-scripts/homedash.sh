#!/bin/bash
PID="homedash.pid"



function start {
    if [ -e $PID ]
    then
        echo "HomeDash is already running. If it is not, delete the file $PID"
    else
        # main class name
        BIN_NAME="com.ftpix.homedash.app.App"

        LIB_PATH=lib
        BIN_PATH=bin
        PLUGIN_PATH=plugins


        PATH=$JAVA_HOME/bin/:$PATH:bin/:.:

        CLASSPATH=$CLASSPATH:cfg/:
        for filename in `ls lib|grep jar`
        do
                CLASSPATH=$CLASSPATH$LIB_PATH/$filename:
        done
        for filename in `ls bin|grep jar`
        do
                CLASSPATH=$CLASSPATH$BIN_PATH/$filename:
        done
        for filename in `ls plugins|grep jar`
        do
                CLASSPATH=$CLASSPATH$PLUGIN_PATH/$filename:
        done

        CLASSPATH=$CLASSPATH.:


        echo "****************"
        echo $CLASSPATH
        echo "****************"


        export CLASSPATH=$CLASSPATH

        java  --class-path $CLASSPATH $BIN_NAME
    fi
}

function stop {
    if [ -e $PID ]
    then
        RUNNING_PID=`cat $PID`
        echo $RUNNING_PID
        kill -9 $RUNNING_PID
        rm $PID
        echo "Homedash stopped"
    else
        echo "Homedash is not running"
    fi
}




if [ $# -eq 0 ]
then
    echo "Argument needed start|restart|stop"
elif [ $1 == "start" ]
then
    start
elif [ $1 == "restart" ]
then
    stop
    start
elif [ $1 == "stop" ]
then
    stop
fi
