#!/bin/bash

[ -z "$TELEGRAM_TOKEN" ] && echo "Need to set TELEGRAM_TOKEN env variable" && exit 1;
[ -z "$SECURITY_SECRET" ] && echo "Need to set SECURITY_SECRET env variable" && exit 1;

case "$1" in
run)
    ./gradlew makeDockerFile
    docker-compose -f server/build/docker/docker-compose.yml up --build
;;
*)
  echo "Run as $0 (run)"
  exit 1
;;
esac