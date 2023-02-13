# Luxmed Bot

[![Build Status](https://drone.rdome.net/api/badges/dyrkin/luxmed-bot/status.svg?branch=master)](https://drone.rdome.net/dyrkin/luxmed-bot)
[![Docker Hub](https://img.shields.io/badge/image-latest-blue.svg?logo=docker&style=flat)](https://hub.docker.com/r/eugenezadyra/luxmed-bot/tags/)

Non official Telegram bot for **Portal Pacjenta LUX MED**.

### Overview 
Luxmed Bot can help you to book a visit to a doctor, create term monitoring, view upcoming appointments and visit history.

It is available here [@luxmedbot](https://telegram.me/luxmedbot), but you can install your instance.

![Screenshot](screenshot.png)

### Installation

1. Create telegram bot using [@BotFather](https://telegram.me/botfather)
2. Install **docker** and **docker-compose** (install **docker-machine** if you are on Mac)
3. Depending on your platform download:
    - [docker-compose.xml](https://raw.githubusercontent.com/dyrkin/luxmed-booking-service/master/docker/docker-compose.yml) 
    - [docker-compose-arm64.xml](https://raw.githubusercontent.com/dyrkin/luxmed-booking-service/master/docker/docker-compose-arm64.yml)
4. Download [secrets.env.template](https://raw.githubusercontent.com/dyrkin/luxmed-booking-service/master/docker/secrets.env.template) 
to the same folder and rename it to **secrets.env**
5. Edit **secrets.env** by specifying your **TELEGRAM_TOKEN** and **SECURITY_SECRET**
6. Start the application by running commands:
    ```bash
    $ docker-compose pull
    $ docker-compose up
    ```
11. Send `/start` command to your bot

### Develop run

1. Run `docker-compose up` to launch PostgreSQL database
2. Set env `TELEGRAM_TOKEN=YOUR_TOKEN`
3. Run `Boot.scala` app



