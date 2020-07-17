#!/bin/bash -l

echo "

----------------------------------------------------------------
   ____                   ______
  / __ \                 |  ____|
 | |  | |_ __   ___ _ __ | |__  _   _  ___  ___
 | |  | | '_ \ / _ \ '_ \|  __|| | | |/ _ \/ __|
 | |__| | |_) |  __/ | | | |___| |_| |  __/\__ \.
  \____/| .__/ \___|_| |_|______\__, |\___||___/
        | |                      __/ |
        |_|                     |___/

Openeyes is an AGPL v3 OpenSource Electronic Patient Record.
Brought to you by the Apperta Foundation (https://apperta.org/)
See the following urls for more info
- https://openeyes.org.uk/
- https://github.com/openeyes/openeyes
- https://github.com/appertafoundation/openeyes

----------------------------------------------------------------

****************************************************************
**************       DICOM Processor Service      **************
****************************************************************

DATABASE_HOST= $DATABASE_HOST:$DATABASE_PORT
DATABASE_USER= $DATABASE_USER
DATABASE_NAME= $DATABASE_NAME

***************************************************************

"
export DEBIAN_FRONTEND=noninteractive
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/lib:/usr/glibc-compat/lib:/usr/local/lib:/lib
echo "Setting Timezone to ${TZ:-'Europe/London'}"
# Set system timezone
grep -q "$TZ" /etc/timezone >/dev/null
[ $? = 1 ] && { ln -sf /usr/share/zoneinfo/${TZ:-Europe/London} /etc/localtime && echo ${TZ:-'Europe/London'} > /etc/timezone; } || :

# Use docker secret as DB password, or fall back to environment variable
[ -f /run/secrets/DATABASE_PASS ] && dbpassword="$(</run/secrets/DATABASE_PASS)" || dbpassword=${DATABASE_PASS:-""}
[ ! -z $dbpassword ] && dbpassword="-p$dbpassword" || dbpassword="-p''" # Add -p to the beginning of the password (workaround for blank passwords)

# Test to see if database and other hosts are available before continuing.
# hosts can be specified as an environment variable WAIT_HOSTS with a comma separated list of host:port pairs to wait for
# The wait command is from https://github.com/ufoscout/docker-compose-wait
# The following additional options are available:
#  # WAIT_HOSTS: comma separated list of pairs host:port for which you want to wait.
#  # WAIT_HOSTS_TIMEOUT: max number of seconds to wait for the hosts to be available before failure. The default is 30 seconds.
#  # WAIT_BEFORE_HOSTS: number of seconds to wait (sleep) before start checking for the hosts availability
#  # WAIT_AFTER_HOSTS: number of seconds to wait (sleep) once all the hosts are available
#  # WAIT_SLEEP_INTERVAL: number of seconds to sleep between retries. The default is 1 second.
# Note that we always add the database server to the list
[ -z $WAIT_HOSTS ] && export WAIT_HOSTS="${DATABASE_HOST:-'localhost'}:${DATABASE_PORT:-'3306'}" || export WAIT_HOSTS="${DATABASE_HOST:-'localhost'}:${DATABASE_PORT:-'3306'},$WAIT_HOSTS"
echo "Waitng for host dependencies to become available..."
if ! /wait = 1; then 
  echo "Not all dependent hosts were contactable. Exiting."
  exit; 
fi

switches="-sf /routineLibrary/ -rq ${PROCESSOR_QUEUE_NAME} -sy ${SYNCHRONIZE_ROUTINE_DELAY} -rq ${RETRY_DATABASE_CONNECTION}"


# If a shutdown after (minutes) has been specified, then pass this to the processor. It will then run for x minutes before automatically shutting down
[ $PROCESSOR_SHUTDOWN_AFTER -gt 0 ] && switches="$switches -sa $PROCESSOR_SHUTDOWN_AFTER" || :

# Start processor
echo "Starting opeyes DicomProcessor process..."
echo ""
echo "***************************************************************"
echo "**       -= END OF PAYLOAD PROCESSOR STARTUP SCRIPT =-       **"
echo "***************************************************************"


$PROJROOT/appassembler/bin/dicomEngine $switches
