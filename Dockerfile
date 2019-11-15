## Builds an OpenEyes DicomProcessor service instance.

ARG MAVEN_TAG=3.6-ibmjava-alpine
ARG BUILD_PROJROOT="/dicomprocessor"
ARG TIMEZONE="Europe/London"

FROM maven:$MAVEN_TAG AS builder

ARG MAVEN_TAG
ARG BUILD_PROJROOT
ARG DEBIAN_FRONTEND=noninteractive
ARG TIMEZONE

## The folder that context files will be copied to
ENV PROJROOT="$BUILD_PROJROOT"
ENV TZ=${TIMEZONE}

COPY settings.xml /usr/share/maven/conf
# set up folders
COPY . ${PROJROOT}
RUN cd $PROJROOT \
    && mvn package


ENTRYPOINT ["/init.sh"]

FROM maven:$MAVEN_TAG

ARG MAVEN_TAG
ARG BUILD_PROJROOT
ARG DEBIAN_FRONTEND=noninteractive
ARG TIMEZONE

RUN apk add --update tesseract-ocr-dev

## API connection details.
## This is a connection to the openeyes web service
## Defaults to assuming web service will be running on the same docker host on port 80
## But it is recommended to override this
## It is STRONGLY recommended to use docker secrets for the API_PASSWORD
ENV API_HOST="host.docker.internal"
ENV API_PORT=80
ENV API_USER=admin
ENV API_PASSWORD=admin

## Used by some scripts to determine if they are runing in a container
ENV DOCKER_CONTAINER="TRUE"

## Database connection credentials:
## It is STRONGLY recommended to change the password for production environments
## It is STRONGLY recommended to use docker secrets for DATABASE_PASS, rather than env variables
## defaults are db; openeyes; 3306; openeyes; openeyes
ENV DATABASE_HOST="db"
ENV DATABASE_NAME="openeyes"
ENV DATABASE_PASS="openeyes"
ENV DATABASE_PORT="3306"
ENV DATABASE_USER="openeyes"

ENV PROCESSOR_QUEUE_NAME="dicom_queue"
ENV PROCESSOR_SHUTDOWN_AFTER=0

## The folder that context files will be copied to
ENV PROJROOT="$BUILD_PROJROOT"

# This option will retry a database connection for X minutes before failing if a connection has been failed to establish
ENV RETRY_DATABASE_CONNECTION=2

# Number of minutes delay when the engine should synchronize the specified scriptFileLocation with the routine_library table 
ENV SYNCHRONIZE_ROUTINE_DELAY=99999

ENV TZ=${TIMEZONE}

ENV WAIT_HOSTS_TIMEOUT=1500
ENV WAIT_SLEEP_INTERVAL=2

# Copy compiled files
COPY --from=builder ${PROJROOT}/target/ ${PROJROOT}
COPY --from=builder ${PROJROOT}/src/main/resources/routineLibrary/ /routineLibrary
COPY --from=builder ${PROJROOT}/src/main/resources/tessdata/ /tessdata

# Add the init script
COPY .docker_build/init.sh /init.sh
COPY .docker_build/wait /wait
RUN chmod a+rx /init.sh /wait

VOLUME ["/tmp"]
ENTRYPOINT ["/init.sh"]