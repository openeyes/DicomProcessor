# Payload Processor

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
```
API_DO_HTTPS defaults to 'FALSE', if the API call has to be with HTTPS protocol value needs to be "TRUE"
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
.\target\appassembler\bin\dicomEngine.bat -sf src/main/resources/routineLibrary/ -rq dicom_queue -sa 1 -sy 1
```
## Logging

Logs are saved in root directory logs folder with name applog.txt

# DOCKER BUILD
This project can be packaged as a docker container. Using the provided Dockerfile.

A pre-packaged version is provided on dockerhub as appertaopeneyes/dicomprocessor

## Building

Builds are created using the [Dockerhub Maven base image](https://hub.docker.com/_/maven). All options for that image are available to be used in dicomprocessor.

Additionally, the following build arguments are available:
* `MAVEN_TAG` - The [maven base image](https://hub.docker.com/_/maven) tag to be used for the build.
    * This is useful for building with different JDK versions, etc

**example build command, using latest maven v3.x.x and JDK 11:** 
 `docker build --build-arg MAVEN_TAG=3-jdk-11 -t dicomprocessor .`

## Running

Use standard docker methods for running. A number of ENV options are provided for tuning the behaviour.

Example: `docker run -e DATABASE_HOST=db appertaopeneyes/dicomprocessor`

### Environment options:

* `API_HOST` - Hostname (or IP address) of the openeyes web server hosting the API (defaults to docker host)
* `API_PORT` - Port number to connect to on the API_HOST (default 80)
* `API_USER` - The username for the API access (this should be created as a user account in openeyes)
* `API_PASSWORD` - The password for the API access user (this should be created as a user account in openeyes)

* `DATABASE_HOST` - Hostname (or IP address) of the main openeyes database server
* `DATABASE_PORT` - Port number that the main openeyes database server is accessed on (default=3306)
* `DATABASE_NAME` - Name of the openeyes database (default='openeyes')
* `DATABASE_USER` - Username for accessing the main openeyes database
* `DATABASE_PASS` - Password for accessing the main openeyes database

* `PROCESSOR_QUEUE_NAME` - The queue name for this processor to use (default='dicom_queue'
* `PROCESSOR_SHUTDOWN_AFTER` - If set, the processor will run for the specified number of seconds and then terminate. There is very little reason you'd want to use this! Default is to run indefinitely.
* `SYNCHRONIZE_ROUTINE_DELAY` - minutes delay when the engine should synchronize the scriptFiles with the routine_library table

### Secrets
Note that the following variables support docker secrets, which should be considered the recommended method of suplying credentials:

* `API_USER`
* `API_PASSWORD`
* `DATABASE_USER`
* `DATABASE_PASSWORD`

### Docker Compose
A sample docker-compose.yml file is provided in the project root.

### Custom device scripts (routines)
The container is built with the latest set of device scripts (routines). These are stored in `/routineLibrary`. Overeriding this folder with a bind volume will allow you to use custom scripts.
If you wish to change scripts on the fly, then either restart the container after each script change, or override the `SYNCHRONIZE_ROUTINE_DELAY` Env variable to a low number. e.g, 1
