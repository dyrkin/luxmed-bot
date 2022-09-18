FROM eclipse-temurin:11-jre-focal

RUN apt-get update -y && apt-get install -y netcat # nc command

RUN ln -fs /usr/share/zoneinfo/Europe/Warsaw /etc/localtime
RUN dpkg-reconfigure -f noninteractive tzdata

RUN mkdir -p /app
ADD server.jar /app
ADD run.sh run.sh
ADD version /app/version
RUN chmod +x run.sh
CMD ./run.sh