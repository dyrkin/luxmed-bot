# Luxmed Bot

[![Build Status](https://cloud.drone.io/api/badges/dyrkin/luxmed-bot/status.svg?branch=master)](https://cloud.drone.io/dyrkin/luxmed-bot)
[![Docker Hub](https://img.shields.io/badge/image-latest-blue.svg?logo=docker&style=flat)](https://hub.docker.com/r/eugenezadyra/luxmed-bot/tags/)

Non official Telegram bot for **Portal Pacjenta LUX MED**.

With its help user can book a visit to a doctor, create term monitoring, view upcoming visits and visit history.

It is available here [@luxmedbot](https://telegram.me/luxmedbot)

![Screenshot](screenshot.png)

#### To setup your own telegram bot

1. create telegram bot using [@BotFather](https://telegram.me/botfather)
2. install **docker** and **docker-compose** (install **docker-machine** if you are on Mac)
3. download [docker-compose.xml](https://raw.githubusercontent.com/dyrkin/luxmed-booking-service/master/docker/docker-compose.yml) 
4. download [secrets.env.template](https://raw.githubusercontent.com/dyrkin/luxmed-booking-service/master/docker/secrets.env.template) 
to the same folder and rename it to **secrets.env**
5. edit **secrets.env** by specifying your **TELEGRAM_TOKEN** and **SECURITY_SECRET**
6. start the application by running commands:
    ```bash
    $ docker-compose pull
    $ docker-compose up
    ```
11. send `/start` command to your bot



