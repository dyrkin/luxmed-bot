package com.lbs.bot.telegram

import com.bot4s.telegram.models.InlineKeyboardMarkup
import com.lbs.bot.PollBot
import com.lbs.bot.model._
import com.lbs.bot.telegram.TelegramModelConverters._

class TelegramBot(onCommand: Command => Unit, botToken: String) extends PollBot[TelegramEvent] {

  private val telegramBot = new TelegramClient(onReceive, botToken)
  telegramBot.run()

  def sendMessage(chatId: String, text: String): Unit =
    telegramBot.sendMessage(chatId.toLong, text)

  def sendMessage(chatId: String, text: String, buttons: Option[InlineKeyboard] = None): Unit =
    telegramBot.sendMessage(chatId.toLong, text, replyMarkup = buttons.map(_.mapTo[InlineKeyboardMarkup]))

  def sendEditMessage(chatId: String, messageId: String, buttons: Option[InlineKeyboard]): Unit =
    telegramBot.sendEditMessage(
      chatId.toLong,
      messageId.toInt,
      replyMarkup = buttons.map(_.mapTo[InlineKeyboardMarkup])
    )

  def sendEditMessage(chatId: String, messageId: String, text: String, buttons: Option[InlineKeyboard] = None): Unit =
    telegramBot.sendEditMessage(
      chatId.toLong,
      messageId.toInt,
      text,
      replyMarkup = buttons.map(_.mapTo[InlineKeyboardMarkup])
    )

  def sendFile(chatId: String, filename: String, contents: Array[Byte], caption: Option[String] = None): Unit =
    telegramBot.sendFile(chatId.toLong, filename, contents, caption)

  override protected def onReceive(command: TelegramEvent): Unit = {
    onCommand(command.mapTo[Command])
  }
}
