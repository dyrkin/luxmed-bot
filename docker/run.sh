#!/bin/sh

echo "********************************************************"
echo "Waiting for the database server to start on port 5432"
echo "********************************************************"
while ! `nc -z database 5432`; do sleep 3; done
echo "******** Database Server has started "

echo "********************************************************"
echo "Starting Luxmed Booking Service  "
echo "********************************************************"

cd /app && java -Xmx128m -jar server.jar