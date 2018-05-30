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
package com.lbs.bot

import com.lbs.bot.model._
import com.lbs.common.ModelConverters
import info.mukel.telegrambot4s.models
import info.mukel.telegrambot4s.models.{InlineKeyboardButton, InlineKeyboardMarkup}

package object telegram {

  protected[bot] val TagPrefix = "callback"

  object TelegramModelConverters extends ModelConverters {
    implicit val TelegramCommandToCommandConverter:
      ObjectConverter[TelegramEvent, Command] =
      new ObjectConverter[TelegramEvent, Command] {
        override def convert[Z <: TelegramEvent](data: Z): Command = {
          Command(
            source = MessageSource(TelegramMessageSourceSystem, data.msg.chat.id.toString),
            message = Message(data.msg.messageId.toString, data.msg.text),
            callbackData = data.callbackData
          )
        }
      }

    implicit val TelegramMessageToMessageConverter:
      ObjectConverter[models.Message, Message] =
      new ObjectConverter[models.Message, Message] {
        override def convert[Z <: models.Message](data: Z): Message = {
          Message(data.messageId.toString, data.text)
        }
      }

    implicit val InlineKeyboardToInlineKeyboardMarkup:
      ObjectConverter[InlineKeyboard, InlineKeyboardMarkup] =
      new ObjectConverter[InlineKeyboard, InlineKeyboardMarkup] {
        override def convert[Z <: InlineKeyboard](inlineKeyboard: Z): InlineKeyboardMarkup = {
          val buttons = inlineKeyboard.buttons.map { row =>
            row.map(createInlineKeyboardButton)
          }
          InlineKeyboardMarkup(buttons)
        }
      }

    private def createInlineKeyboardButton(button: Button) = {
      button match {
        case b: TaggedButton => InlineKeyboardButton.callbackData(b.label, tag(b.tag))
        case b: LabeledButton => InlineKeyboardButton.callbackData(b.label, b.label)
      }
    }

    private def tag(name: String): String = TagPrefix + name
  }

}
