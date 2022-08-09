package com.lbs.bot.model

case class Message(messageId: String, text: Option[String] = None)

case class Command(source: MessageSource, message: Message, callbackData: Option[String] = None)
