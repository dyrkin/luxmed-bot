# Luxmed Booking Service

[![Build Status](https://travis-ci.org/dyrkin/luxmed-booking-service.svg?branch=master)](https://travis-ci.org/dyrkin/luxmed-booking-service)

Non official Telegram bot for **Portal Pacjenta LUX MED**.

With its help user can book a visit to a doctor, create term monitoring, view upcoming visits and visit history.

It is available by [@luxmedbot](https://telegram.me/luxmedbot)

![Screenshot](screenshot.png)

#### To setup your own

1. create your own telegram bot using [@BotFather](https://telegram.me/botfather)
2. install jdk8
3. add to .bash_profile 

    ```
    export TELEGRAM_TOKEN="SOME TOKEN"
    export SECURITY_SECRET="SOME SECRET FOR ENCODING USER PASSWORDS"
    ```
4. 
    - install **docker**
    - run using `./docker.sh run`

    **or**

    - install postgres and create db **lbs** with login **lbs** and password **lsb123**
    - run using `./gradlew bootRun`

5. send `/start` command to your bot



