#!/usr/bin/env bash

APP=/app
CONFIG=/app/homedash.properties
rm ${CONFIG}
touch ${CONFIG}

if [ -z ${JAVA_OPTS+x} ]; then
    JAVA_OPTS=""
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
       java ${JAVA_OPTS} -Dconfig.file=$CONFIG -jar homedash.jar
fi


