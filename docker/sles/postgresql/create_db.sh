#!/bin/sh
status=false
SPENT_TIME=0
SLEEP_TIMEOUT=1
CREATE_CMD="CREATE DATABASE $POSTGRES_DB"
if [ -n "$PG_APP_USER" -a "$PG_APP_USER" != "$POSTGRES_USER" ]; then
  CREATE_CMD="$CREATE_CMD OWNER $PG_APP_USER"
fi
GRANT_CMD="GRANT ALL ON DATABASE $POSTGRES_DB TO $PG_APP_USER"

message=`psql -h $POSTGRES_HOST -U $POSTGRES_USER -w -p $POSTGRES_PORT -d postgres -c "$CREATE_CMD" 2>&1`
ALREADY_EXISTS_ERROR_MESSAGE="ERROR:  database \"$POSTGRES_DB\" already exists"
echo $message
while true; do
  if [[ "${#message}" = "${#ALREADY_EXISTS_ERROR_MESSAGE}" ]]; then
    echo "$POSTGRES_DB already present. Skipping creation of $POSTGRES_DB ..."
    # Upgrade case: grant all permissions on database to real non-admin user
    if [ -n "$PG_APP_USER" -a "$PG_APP_USER" != "$POSTGRES_USER" ]; then
        echo "Granting all permissions on $POSTGRES_DB to user $PG_APP_USER"
        psql -h $POSTGRES_HOST -U $POSTGRES_USER -w -p $POSTGRES_PORT -d postgres -c "$GRANT_CMD"
    fi
    exit 0;
  elif [[ "${message}" = "CREATE DATABASE" ]]; then
    echo "Successfully added $POSTGRES_DB database."
    exit 0;
  else
    echo "Failed to create database \"$POSTGRES_DB\". Retrying...."
    sleep ${SLEEP_TIMEOUT}
    SPENT_TIME=$(($SPENT_TIME + $SLEEP_TIMEOUT))
    message=`psql -h $POSTGRES_HOST -U $POSTGRES_USER -w -p $POSTGRES_PORT -d postgres -c "$CREATE_CMD" 2>&1`
  fi
  if [[ "$SPENT_TIME" -ge "$STARTUP_WAIT" ]]; then
    echo "ERROR: Timeout limit reached"
    exit 1
  fi
done
