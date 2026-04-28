FROM eclipse-temurin:25-jre-noble

RUN apt-get update --allow-unauthenticated --allow-insecure-repositories -y  && apt-get install --allow-unauthenticated -y netcat-traditional # nc command

RUN ln -fs /usr/share/zoneinfo/Europe/Warsaw /etc/localtime
RUN dpkg-reconfigure -f noninteractive tzdata

RUN mkdir -p /app
ADD server.jar /app
ADD run.sh run.sh
ADD version /app/version
RUN chmod +x run.sh
CMD ./run.sh