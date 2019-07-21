#!/usr/bin/env bash


APP=/app
CONFIG_FILE=/app/homedash.properties

#####
# user stuff
#####

USER="root"
GROUP="root"
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
	
	# checking if the group addition worked
	if [ $? -eq "0" ]; then
		GROUP="abc"	
	else
		GROUP=$(cat /etc/group | grep ":${GID}:" | awk -F':' '{print $1}')
		echo "Group already exists, using ${GROUP}"
	fi
	
   fi

    useradd -d ${APP} -u ${UID} -g ${GID} abc
    if [ $? -eq "0" ]; then
        USER="abc"	
    else
        USER=$(id -nu ${UID})
	echo "User already exists, using ${USER}"
    fi
    	

fi


rm ${CONFIG_FILE}
touch ${CONFIG_FILE}

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
    echo "port=4567" >> ${CONFIG_FILE}

    echo "cache_path = cache/" >> ${CONFIG_FILE}
    echo "db_path = /data/homedash" >> ${CONFIG_FILE}

    echo "salt = ${SALT}" >> $CONFIG_FILE



    if [ -z ${SECURE+x} ]; then
        echo "secure = false" >> ${CONFIG_FILE}
    else
        echo "secure = ${SECURE}" >> ${CONFIG_FILE}
        # Required only if secure = true, more help: https://uwesander.de/using-your-ssl-certificate-for-your-spark-web-application.html
        if [ -z ${KEY_STORE+x} ]; then
            echo "No key store set"
        else
            echo "key_store = ${KEY_STORE}" >> ${CONFIG_FILE}
        fi

        if [ -z ${KEY_STORE_PASS+x} ]; then
            echo "No key store pass set"
        else
            echo "key_store_pass = ${KEY_STORE_PASS}" >> ${CONFIG_FILE}
         fi
    fi


    if [ -z ${CONFIG+x} ]; then
        CONFIG="";
    fi

    echo "######################################"
    echo "STARTING HOMEDASH"
    cat ${CONFIG_FILE}
    echo "User: ${USER}"
    echo "Group: ${GROUP}"
    echo "Config: ${CONFIG}"
    echo "######################################"

    cd $APP
    chown -R ${USER}:${GROUP} $APP /data

    JAVA_PATH=java

    if [ "${USER}" != "root" ]; then
        echo "Runing su"
        su ${USER} -c "CONFIG='${CONFIG}' ${JAVA_PATH} ${JAVA_OPTS} -Dconfig.file=$CONFIG_FILE -jar homedash.jar"
#        su ${USER} -c 'echo $PATH'

    else
       CONFIG="${CONFIG}" ${JAVA_PATH} ${JAVA_OPTS} -Dconfig.file=${CONFIG_FILE} -jar homedash.jar
    fi
fi


