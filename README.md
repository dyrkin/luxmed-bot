# Luxmed Booking Service

[![Build Status](https://travis-ci.org/dyrkin/luxmed-booking-service.svg?branch=master)](https://travis-ci.org/dyrkin/luxmed-booking-service)

Non official Telegram bot for **Portal Pacjenta LUX MED**.

With its help user can book a visit to a doctor, create term monitoring, view upcoming visits and visit history.

It is available here [@luxmedbot](https://telegram.me/luxmedbot)

![Screenshot](screenshot.png)

#### To setup your own telegram bot

1. create telegram bot using [@BotFather](https://telegram.me/botfather)
2. install **docker** and **docker-compose**
3. create a docker volume and name it **lbs**:
    ```bash
    $ docker volume create lbs
    ```
4. find physical location of the volume:
    ```bash
    docker volume inspect lbs
    ```
    this will produce the following output:
    ```json
    [
        {
            "CreatedAt": "2018-12-05T14:32:35Z",
            "Driver": "local",
            "Labels": {
                "com.docker.compose.project": "docker",
                "com.docker.compose.version": "1.23.1",
                "com.docker.compose.volume": "lbs"
            },
            "Mountpoint": "/var/lib/docker/volumes/lbs/_data",
            "Name": "lbs",
            "Options": null,
            "Scope": "local"
        }
    ]
    ```   
5. using value from **Mountpoint** create **config** folder:
    ```bash
    mkdir /var/lib/docker/volumes/lbs/_data/config
    ```
6. create env file in that folder
    ```bash
    touch /var/lib/docker/volumes/lbs/_data/config/env
    ```
7. edit the file by adding **TELEGRAM_TOKEN** and **SECURITY_SECRET**
    ```bash
    nano /var/lib/docker/volumes/lbs/_data/config/env
    ```
    ```bash
    SECURITY_SECRET=randomly generated string
    TELEGRAM_TOKEN=12345678:telegram token
    ```
8. download [docker-compose.xml](https://raw.githubusercontent.com/dyrkin/luxmed-booking-service/master/docker/docker-compose.yml) 
9. go to folder with downloaded **docker-compose.xml** and run command:
    ```bash
    $ docker-compose up
    ```
10. send `/start` command to your bot



