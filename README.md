# HomeDash

## Requirements

### Build

1. Java SDK 1.8
2. Maven

### Run

1. Java 8 (using openjdk)

## How to build

```
mvn clean compile install package
```

The compiled application will be under web/target/web-{version}-assembly or the zip file can be found in web/target/web-{version}-assembly.zip

## Run from source with Maven

```
mvn -pl web exec:java
```

## Run from distribution

Just run the homedash.sh (linux) or ~~homedash.bat (windows)~~ (doesn't work at the moment)

## Develop plugin

If you're interested to develop a plugin, check DevelopPlugin.md