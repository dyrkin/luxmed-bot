
package com.lbs.bot.telegram

import com.lbs.common.Logger
import info.mukel.telegrambot4s.api.declarative.{Callbacks, Commands}
import info.mukel.telegrambot4s.api.{Polling, TelegramBot => TelegramBotBase}
import info.mukel.telegrambot4s.methods._
import info.mukel.telegrambot4s.models._

import scala.concurrent.Future

class TelegramClient(onReceive: TelegramEvent => Unit, botToken: String) extends TelegramBotBase with Polling with Commands with Callbacks with Logger {

  override def token: String = botToken

  def sendMessage(chatId: Long, text: String): Future[Message] =
    request(SendMessage(chatId, text, parseMode = Some(ParseMode.HTML)))

  def sendMessage(chatId: Long, text: String, replyMarkup: Option[InlineKeyboardMarkup] = None): Future[Message] =
    request(SendMessage(chatId, text, parseMode = Some(ParseMode.HTML), replyMarkup = replyMarkup))

  def sendEditMessage(chatId: Long, messageId: Int, replyMarkup: Option[InlineKeyboardMarkup]): Future[Either[Boolean, Message]] =
    request(EditMessageReplyMarkup(Some(chatId), Some(messageId), replyMarkup = replyMarkup))

  def sendEditMessage(chatId: Long, messageId: Int, text: String, replyMarkup: Option[InlineKeyboardMarkup] = None): Future[Either[Boolean, Message]] =
    request(EditMessageText(Some(chatId), Some(messageId), text = text, parseMode = Some(ParseMode.HTML), replyMarkup = replyMarkup))

  def sendFile(chatId: Long, filename: String, contents: Array[Byte], caption: Option[String] = None): Future[Message] =
    request(SendDocument(chatId, InputFile(filename, contents), caption))


  override def receiveMessage(msg: Message): Unit = {
    debug(s"Received telegram message: $msg")
    onReceive(TelegramEvent(msg, None))
  }

  onCallbackWithTag(TagPrefix) { implicit cbq =>
    debug(s"Received telegram callback: $cbq")
    ackCallback()
    for {
      data <- cbq.data.map(_.stripPrefix(TagPrefix))
      msg <- cbq.message
    } {
      onReceive(TelegramEvent(msg, Some(data)))
    }
  }
}