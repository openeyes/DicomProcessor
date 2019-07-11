## Builds an OpenEyes DicomProcessor service instance.

ARG MAVEN_TAG=3-alpine
ARG BUILD_PROJROOT="/dicomprocessor"

FROM maven:$MAVEN_TAG

ARG MAVEN_TAG
ARG BUILD_PROJROOT
ARG DEBIAN_FRONTEND=noninteractive
ARG TIMEZONE="Europe/London"

## The folder that context files will be copied to
ENV PROJROOT="$BUILD_PROJROOT"

## Used by some scripts to determine if they are runing in a container
ENV DOCKER_CONTAINER="TRUE"

ENV WAIT_HOSTS_TIMEOUT=1500
ENV WAIT_SLEEP_INTERVAL=2

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

# set up folders
COPY . ${PROJROOT}
RUN cd $PROJROOT \
    && mvn package

# Add the init script
RUN chmod a+rx ${PROJROOT}/.docker_build/* \
  && mv ${PROJROOT}/.docker_build/wait /wait \
  && mv ${PROJROOT}/.docker_build/init.sh /init.sh

ENTRYPOINT ["/init.sh"]