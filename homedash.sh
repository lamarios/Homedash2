#!/bin/sh

mvn clean install compile eclipse:eclipse
mvn exec:java -pl web -Dexec.mainClass=com.ftpix.homedash.app.App
