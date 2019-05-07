# DicomProcessor

Java code for processing medical device files

## Compilation

To compile you'll need JDK version 1.8 or higher and Apache Maven

https://maven.apache.org/install.html

Run command in root of the project
```
mvn package
```

## Database configuration
This is either done from docker secrets or environment variables

Docker secrets are being read from run/secrets/

List of needed parameters:
```
DATABASE_HOST
```
```
DATABASE_PORT
```
```
DATABASE_NAME
```

Which are used in such matter:
```
"jdbc:mysql://" + host + ":" + port + "/" + databaseName;
```

## Patient Search Api Configuration
This is done with envronment variables and api is used to look for a patient with a call to openeyes where
it handles hospital number regexs and calls PAS

List of needed environment variables:
```
API_HOST
```
```
API_PORT
```
```
API_USER
```
```
API_PASSWORD
```
## Required command line arguments

### shutdownAfterMinutes
```
-sa 
```
 It's an optional value and when not provided will run as service without stopping.
 An option that will be used to determine how long the engine should run and will shutdown after the specified amount of minutes

Example value : 60

### requestQueue
```
-rq
```
The request queue of the table request_queue in the database that should be processed

Example value : "dicom_queue"
### scriptFileLocation
```
-sf 
```
The location of the directory of the javascript scripts that the engine will use to process the medical device files

Example value : "src/main/resources/routineLibrary/"

### synchronizeRoutineDelay
```
-sy 
```
minutes delay when the engine should synchronize the specified scriptFileLocation with the routine_library table

Example value : 1

###  retryDatabaseConnectionForMinutes
```
-rd
```
To help with the containers starting out of order this option will retry a database connection for X minutes before failing if a connection has been failed to establish

Example value : 1

## Execution

After the project is built the file that has to be run is under target/appassembler/bin/

For Windows the file is called
```
dicomEngine.bat
```
For Linux the file is called
```
dicomEngine
```

Example of execution in windows:
```
.\target\appassembler\bin\app.bat -sf src/main/resources/routineLibrary/ -rq dicom_queue -sa 1 -sy 1
```
## Logging

Logs are saved in rott directory logs folder with name applog.txt
