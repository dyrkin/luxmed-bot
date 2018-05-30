/**
 * MIT License
 *
 * Copyright (c) 2018 Yevhen Zadyra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lbs.bot.telegram

import com.lbs.common.Logger
import info.mukel.telegrambot4s.api.declarative.{Callbacks, Commands}
import info.mukel.telegrambot4s.api.{Polling, TelegramBot => TelegramBotBase}
import info.mukel.telegrambot4s.methods.{EditMessageReplyMarkup, EditMessageText, ParseMode, SendMessage}
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


  override def receiveMessage(msg: Message): Unit = {
    LOG.debug(s"Received telegram message: $msg")
    onReceive(TelegramEvent(msg, None))
  }

  onCallbackWithTag(TagPrefix) { implicit cbq =>
    LOG.debug(s"Received telegram callback: $cbq")
    ackCallback()
    for {
      data <- cbq.data.map(_.stripPrefix(TagPrefix))
      msg <- cbq.message
    } {
      onReceive(TelegramEvent(msg, Some(data)))
    }
  }
}