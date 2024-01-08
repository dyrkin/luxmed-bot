#!/bin/sh

echo "********************************************************"
echo "Waiting for the database server to start on host ${DB_HOST:-localhost} and port ${DB_PORT:-5432}"
echo "********************************************************"
while ! `nc -z ${DB_HOST:-localhost} ${DB_PORT:-5432}`; do sleep 3; done
echo "******** Database Server has been started "

echo "********************************************************"
echo "Starting Luxmed Booking Service  "
echo "********************************************************"

cd /app && java -Xmx128m -jar server.jar