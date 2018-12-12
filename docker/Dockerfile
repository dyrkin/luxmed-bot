FROM openjdk:8-jdk-alpine

RUN apk update && apk upgrade && apk add netcat-openbsd
RUN mkdir -p /app
ADD server.jar /app
ADD run.sh run.sh
ADD version /app/version
RUN chmod +x run.sh
RUN sudo echo "Europe/Warsaw" > /etc/timezone
CMD ./run.sh