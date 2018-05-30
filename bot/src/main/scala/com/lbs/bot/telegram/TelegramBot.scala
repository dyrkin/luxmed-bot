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

import com.lbs.bot.PollBot
import com.lbs.bot.model._
import com.lbs.bot.telegram.TelegramModelConverters._
import info.mukel.telegrambot4s.models.InlineKeyboardMarkup

class TelegramBot(onCommand: Command => Unit, botToken: String) extends PollBot[TelegramEvent] {

  private val telegramBot = new TelegramClient(onReceive, botToken)
  telegramBot.run()

  def sendMessage(chatId: String, text: String): Unit =
    telegramBot.sendMessage(chatId.toLong, text)

  def sendMessage(chatId: String, text: String, buttons: Option[InlineKeyboard] = None): Unit =
    telegramBot.sendMessage(chatId.toLong, text, replyMarkup = buttons.map(_.mapTo[InlineKeyboardMarkup]))

  def sendEditMessage(chatId: String, messageId: String, buttons: Option[InlineKeyboard]): Unit =
    telegramBot.sendEditMessage(chatId.toLong, messageId.toInt, replyMarkup = buttons.map(_.mapTo[InlineKeyboardMarkup]))

  def sendEditMessage(chatId: String, messageId: String, text: String, buttons: Option[InlineKeyboard] = None): Unit =
    telegramBot.sendEditMessage(chatId.toLong, messageId.toInt, text, replyMarkup = buttons.map(_.mapTo[InlineKeyboardMarkup]))

  override protected def onReceive(command: TelegramEvent): Unit = {
    onCommand(command.mapTo[Command])
  }
}