#!/bin/sh

echo "********************************************************"
echo "Waiting for the database server to start on port 5432"
echo "********************************************************"
while ! `nc -z database 5432`; do sleep 3; done
echo "******** Database Server has started "

echo "********************************************************"
echo "Reading env file"
echo "********************************************************"

env="/lbs/config/env"

if [ -f "$env" ]
then
  while IFS='=' read -r key value
  do
    key=$(echo ${key} | tr '.' '_')
    export ${key}=${value}
  done < "$env"

else
  echo "$env not found."
  exit 1
fi

echo "********************************************************"
echo "Starting Luxmed Booking Service  "
echo "********************************************************"

cd /app && java -jar server.jar
