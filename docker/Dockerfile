FROM eclipse-temurin:25-jre-noble

RUN apt-get update --allow-unauthenticated --allow-insecure-repositories -y  && apt-get install --allow-unauthenticated -y netcat-traditional # nc command

RUN ln -fs /usr/share/zoneinfo/Europe/Warsaw /etc/localtime
RUN dpkg-reconfigure -f noninteractive tzdata

RUN mkdir -p /app
ADD run.sh run.sh
RUN chmod +x run.sh
ADD server.jar /app
ADD version /app/version
CMD ./run.sh