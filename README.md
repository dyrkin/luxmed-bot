# Luxmed Booking Service

[![Build Status](https://travis-ci.org/dyrkin/luxmed-booking-service.svg?branch=master)](https://travis-ci.org/dyrkin/luxmed-booking-service)

Non official Telegram bot for **Portal Pacjenta LUX MED**.

With its help user can book a visit to a doctor, create term monitoring, view upcoming visits and visit history.

It is available here [@luxmedbot](https://telegram.me/luxmedbot)

![Screenshot](screenshot.png)

#### To setup your own telegram bot

1. create telegram bot using [@BotFather](https://telegram.me/botfather)
2. install **docker** and **docker-compose**
3. clone repository and build project:
    ```bash
    $ ./gradlew prepare
    ```
4. create a docker volume and name it **lbs**:
    ```bash
    $ docker volume create lbs
    ```
5. find physical location of the volume:
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
6. using value from **Mountpoint** create **config** folder:
    ```bash
    mkdir /var/lib/docker/volumes/lbs/_data/config
    ```
7. create env file in that folder
    ```bash
    touch /var/lib/docker/volumes/lbs/_data/config/env
    ```
8. edit file and add **TELEGRAM_TOKEN** and **SECURITY_SECRET**
    ```bash
    nano /var/lib/docker/volumes/lbs/_data/config/env
    ```
    ```bash
    SECURITY_SECRET=randomly generated string
    TELEGRAM_TOKEN=12345678:telegram token
    ```
4. go to the **docker** folder inside the project and run command
    ```bash
    $ docker-compose up
    ```
5. send `/start` command to your bot



