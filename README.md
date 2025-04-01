# openoffice03
Open Office Loader POC

## Build

```
$ mvn clean compile
```

## Execute

Set environment variable

```
$ export UNO_PATH=/usr/lib64/libreoffice/program
$ java -jar target/openoffice03-1.0-SNAPSHOT.jar
```

Expected output

```
Connected to a running office ...
remote ServiceManager is available
```
