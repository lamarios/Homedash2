# HomeDash

## Requirements

### Build

1. Java SDK 1.10
2. Maven

### Run

1. Java 10

## How to build

```
mvn clean compile install package
```

The compiled application will be under web/target/Homedash-{version}.jar

## Run from source with Maven

```
mvn -pl web exec:java
```

## Run from distribution

### From JAR file

First time you run the application, you'll need to generate a config file.
```
java -jar Homedash-{version}.jar -create-config
```

This will create a config file that you can modify in your current working directory.

You can then run jav -Dconfig.file=./homedash.properties -jar Homedash-{version}.jar

### From docker

You can run homedash using docker.

```
docker run -t --name homedash \
        -v "/etc/localtime:/etc/localtime:ro" \
        -v "/your/path/to/save/data:/data" \
        -e "SALT=somerandomstring" \
        -p "4567:4567" \
        gonzague/homedash
```


To run with SSL you'll need  to add the following environment variables to your container:
```
SECURE=true
KEY_STORE=path to the jks file
KEY_STORE_PASS=password to your jks file
```

Note that running Homedash in docker will have reduced feature when it comes to system monitoring due to the nature of docker containers.

## Develop plugin

If you're interested to develop a plugin, check DevelopPlugin.md