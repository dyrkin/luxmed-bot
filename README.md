# Luxmed Booking Service

Non official bot for **Portal Pacienta LUX MED**.

With its help user can book a visit to a doctor, create term monitoring, view upcoming visits and visit history.

It is available by [@luxmedbot](https://telegram.me/luxmedbot)

#### To setup your own

1. create your own telegram bot using [@BotFather](https://telegram.me/botfather)
2. add to .bash_profile 

    ```
    export TELEGRAM_TOKEN="SOME TOKEN"
    export SECURITY_SECRET="SOME SECRET FOR ENCODING USER PASSWORDS"
    ```
3. install postgres and create db **lbs** with login **lbs** and password **lsb123**
4. run using `./gradlew bootRun`
5. send `/start` to your bot



