
package com.lbs.bot

import com.lbs.bot.model._
import com.lbs.bot.telegram.TelegramBot
import com.lbs.common.Logger

class Bot(telegram: TelegramBot /* other bots */) extends Logger {
  def sendMessage(source: MessageSource, text: String): Unit =
    resolveAdapter(source).sendMessage(source.chatId, text)

  def sendMessage(source: MessageSource, text: String, inlineKeyboard: Option[InlineKeyboard] = None): Unit =
    resolveAdapter(source).sendMessage(source.chatId, text, inlineKeyboard)

  def sendEditMessage(source: MessageSource, messageId: String, inlineKeyboard: Option[InlineKeyboard]): Unit =
    resolveAdapter(source).sendEditMessage(source.chatId, messageId, inlineKeyboard)

  def sendEditMessage(source: MessageSource, messageId: String, text: String, inlineKeyboard: Option[InlineKeyboard] = None): Unit =
    resolveAdapter(source).sendEditMessage(source.chatId, messageId, text, inlineKeyboard)

  def sendFile(source: MessageSource, filename: String, contents: Array[Byte], caption: Option[String] = None): Unit =
    resolveAdapter(source).sendFile(source.chatId, filename, contents, caption)

  private def resolveAdapter(source: MessageSource): PollBot[_] =
    source.sourceSystem match {
      case TelegramMessageSourceSystem => telegram
      case sourceSystem =>
        sys.error(s"Unsupported source system $sourceSystem")
    }
}
