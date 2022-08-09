FROM amd64/openjdk:11-jre-slim

RUN apk update && apk upgrade && apk add netcat-openbsd tzdata && apk add --no-cache nss
RUN mkdir -p /app
ENV TZ Europe/Warsaw
RUN echo "Europe/Warsaw" > /etc/timezone
ADD server.jar /app
ADD run.sh run.sh
ADD version /app/version
RUN chmod +x run.sh
CMD ./run.sh