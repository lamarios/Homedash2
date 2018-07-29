#!/usr/bin/env bash


CONFIG=/config/homedash.properties
rm ${CONFIG}
touch ${CONFIG}

if [ -z ${SALT+x} ]; then
    echo "Missing salt string environment variable"
else

    #wrting config
    echo "port=4567" >> ${CONFIG}

    echo "cache_path = cache/" >> ${CONFIG}
    echo "db_path = /config/homedash" >> ${CONFIG}

    echo "salt = ${SALT}" >> $CONFIG



    if [ -z ${SECURE+x} ]; then
        echo "secure = false" >> ${CONFIG}
    else
        echo "secure = ${SECURE}" >> ${CONFIG}
        # Required only if secure = true, more help: https://uwesander.de/using-your-ssl-certificate-for-your-spark-web-application.html
        if [ -z ${KEY_STORE+x} ]; then
            echo "key_store = ${KEY_STORE}" >> ${CONFIG}
        fi

        if [ -z ${KEY_STORE_PASS+x} ]; then
            echo "key_store_pass = ${KEY_STORE_PASS}" >> ${CONFIG}
         fi
    fi

    java -Dconfig.file=/config/homedash.properties -jar /app/Homedash.jar
fi


