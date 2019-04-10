
package com.lbs.bot.telegram

import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.api.{AkkaTelegramBot, Polling}
import com.bot4s.telegram.clients.AkkaHttpClient
import com.bot4s.telegram.methods._
import com.bot4s.telegram.models.{InlineKeyboardMarkup, InputFile, Message}
import com.lbs.common.Logger

import scala.concurrent.Future

class TelegramClient(onReceive: TelegramEvent => Unit, botToken: String) extends AkkaTelegramBot with Polling with Commands with Callbacks with Logger {

  val client = new AkkaHttpClient(botToken)

  def sendMessage(chatId: Long, text: String): Future[Message] =
    loggingRequest(SendMessage(chatId, text, parseMode = Some(ParseMode.HTML)))

  def sendMessage(chatId: Long, text: String, replyMarkup: Option[InlineKeyboardMarkup] = None): Future[Message] =
    loggingRequest(SendMessage(chatId, text, parseMode = Some(ParseMode.HTML), replyMarkup = replyMarkup))

  def sendEditMessage(chatId: Long, messageId: Int, replyMarkup: Option[InlineKeyboardMarkup]): Future[Either[Boolean, Message]] =
    loggingRequest(EditMessageReplyMarkup(Some(chatId), Some(messageId), replyMarkup = replyMarkup))

  def sendEditMessage(chatId: Long, messageId: Int, text: String, replyMarkup: Option[InlineKeyboardMarkup] = None): Future[Either[Boolean, Message]] =
    loggingRequest(EditMessageText(Some(chatId), Some(messageId), text = text, parseMode = Some(ParseMode.HTML), replyMarkup = replyMarkup))

  def sendFile(chatId: Long, filename: String, contents: Array[Byte], caption: Option[String] = None): Future[Message] =
    loggingRequest(SendDocument(chatId, InputFile(filename, contents), caption))

  private def loggingRequest[R: Manifest](req: Request[R]): Future[R] = {
    debug(s"Sending telegram request: $req")
    request(req)
  }


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