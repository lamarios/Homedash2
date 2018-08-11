#!/usr/bin/env bash


APP=/app
CONFIG=/app/homedash.properties

#####
# user stuff
#####

ROOT=1
if [ -z ${UID+x} ]; then
    echo "Running as ROOT"
    UID=0
else
   echo "Creating user for id ${UID} and group ${GID}"

   if [ -z ${GID+x} ]; then
        echo "Group ID = 0"
        GID=0
   else
        groupadd -g $GID abc
   fi

    ROOT=0
    useradd -d ${APP} -u ${UID} -g ${GID} abc
fi


rm ${CONFIG}
touch ${CONFIG}

if [ -z ${JAVA_OPTS+x} ]; then
    JAVA_OPTS=""
fi

if [ -z ${DEBUG+x} ]; then
    echo "Runing in normal mode"
else
    echo "Running debug mode"
    JAVA_OPTS="${JAVA_OPTS} -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=4570 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
fi


if [ -z ${SALT+x} ]; then
    echo "Missing salt string environment variable"
else


    ls ${APP}
    #wrting config
    echo "port=4567" >> ${CONFIG}

    echo "cache_path = cache/" >> ${CONFIG}
    echo "db_path = /data/homedash" >> ${CONFIG}

    echo "salt = ${SALT}" >> $CONFIG



    if [ -z ${SECURE+x} ]; then
        echo "secure = false" >> ${CONFIG}
    else
        echo "secure = ${SECURE}" >> ${CONFIG}
        # Required only if secure = true, more help: https://uwesander.de/using-your-ssl-certificate-for-your-spark-web-application.html
        if [ -z ${KEY_STORE+x} ]; then
            echo "No key store set"
        else
            echo "key_store = ${KEY_STORE}" >> ${CONFIG}
        fi

        if [ -z ${KEY_STORE_PASS+x} ]; then
            echo "No key store pass set"
        else
            echo "key_store_pass = ${KEY_STORE_PASS}" >> ${CONFIG}
         fi
    fi

    echo "######################################"
    echo "STARTING HOMEDASH"
    cat ${CONFIG}
    echo "######################################"


    cd $APP
    if [ "$ROOT" -eq "0" ]; then
        chown -R abc:abc $APP /data
        runuser -l abc -c "java ${JAVA_OPTS} -Dconfig.file=$CONFIG -jar homedash.jar"

    else
        chown -R root:root $APP /data
        java ${JAVA_OPTS} -Dconfig.file=$CONFIG -jar homedash.jar
    fi
fi


