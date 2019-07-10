## Builds an OpenEyes DicomProcessor service instance.

ARG OS_VERSION=3-alpine
ARG BUILD_PROJROOT="/dicomprocessor"

FROM maven:$OS_VERSION

ARG OS_VERSION
ARG BUILD_PROJROOT
ARG DEBIAN_FRONTEND=noninteractive
ARG TIMEZONE="Europe/London"

## The folder that context files will be copied to
ENV PROJROOT="$BUILD_PROJROOT"

## Used by some scripts to determine if they are runing in a container
ENV DOCKER_CONTAINER="TRUE"

ENV SSH_PRIVATE_KEY=""
ENV TZ=$TIMEZONE

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


# # Set timezone, add common packes, apt clean at the end to minimise layer size
# RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
#  && apt-get update && apt-get install --no-install-recommends -y \
#     #git-core \
#     #mariadb-client \
#     maven \
#     nano \
#     #ssh-client \
#     sudo \
#   && apt-get autoremove -y \
#   && rm -rf /var/lib/apt/lists/* \
#   && apt-get clean -y \
#   && rm -rf /var/www/html/* 
#    #&& git config --global core.fileMode false

  # set up folders
  COPY . ${PROJROOT}
  RUN cd $PROJROOT \
      && mvn package

  # Add the init script
  RUN chmod a+rx ${PROJROOT}/.docker_build/* \
    && mv ${PROJROOT}/.docker_build/wait /wait \
    && mv ${PROJROOT}/.docker_build/init.sh /init.sh

  ENTRYPOINT ["/init.sh"]