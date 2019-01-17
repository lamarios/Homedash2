# HomeDash

## Requirements

### Build

1. Java SDK 1.11
2. Maven

### Run

1. Java 11

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

You can then run 
```
java -Dconfig.file=./homedash.properties -jar Homedash-{version}.jar
```

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
Environment Variables:

| Variable | Description | Required |
| ---------- | ------------- | ---------- |
| SALT | A random string used for authentication and other hashes | YES |
| UID | if you want to run the container under a different user id. Runs as root (0) by default | no |
| GID | The group id to run the container under. Runs as root (0) by default | no |
| JAVA_OPTS | configure the JVM | no |
| SECURE | "true" or "false" (default) to enable HTTPS  | no |
| KEY_STORE | if SECURE="true" you need to specified the path of your keystore (jks) file within the container after you mounted it | no |
| KEY_STORE_PASS | the password to your key store file | no | 
| DEBUG | Set to "true" to allow connection to the JMX to get visual vm debugging, if enabled you need to add the port map the port 4570 of the container as well. | no | 
| CONFIG | You can define all the plugins and boards using this variable. It is a JSON format. You can generate this JSON from an existing instance of homedash at the url http://yourhomedashinstall:port/export-config | no |


Note that running Homedash in docker will have reduced feature when it comes to system monitoring due to the nature of docker containers.

## Develop plugin

If you're interested to develop a plugin, check DevelopPlugin.md
