
package com.lbs.bot

import com.bot4s.telegram.models.{InlineKeyboardButton, InlineKeyboardMarkup, Message => BMessage}
import com.lbs.bot.model._
import com.lbs.common.ModelConverters

package object telegram {

  protected[bot] val TagPrefix = "callback"

  object TelegramModelConverters extends ModelConverters {
    implicit val TelegramCommandToCommandConverter:
      ObjectConverter[TelegramEvent, Command] =
      (data: TelegramEvent) => {
        Command(
          source = MessageSource(TelegramMessageSourceSystem, data.msg.chat.id.toString),
          message = Message(data.msg.messageId.toString, data.msg.text),
          callbackData = data.callbackData
        )
      }

    implicit val TelegramMessageToMessageConverter:
      ObjectConverter[BMessage, Message] =
      (data: BMessage) => {
        Message(data.messageId.toString, data.text)
      }

    implicit val InlineKeyboardToInlineKeyboardMarkup:
      ObjectConverter[InlineKeyboard, InlineKeyboardMarkup] =
      (inlineKeyboard: InlineKeyboard) => {
        val buttons = inlineKeyboard.buttons.map { row =>
          row.map(createInlineKeyboardButton)
        }
        InlineKeyboardMarkup(buttons)
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
