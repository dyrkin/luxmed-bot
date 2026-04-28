package com.lbs.bot.telegram

import com.bot4s.telegram.models.{InlineKeyboardButton, InlineKeyboardMarkup, Message as BMessage}
import com.lbs.bot.model.*
import com.lbs.common.ModelConverters

protected[bot] val TagPrefix = "callback"

object TelegramModelConverters extends ModelConverters {
  given TelegramCommandToCommandConverter: ObjectConverter[TelegramEvent, Command] =
    (data: TelegramEvent) =>
      Command(
        source = MessageSource(TelegramMessageSourceSystem, data.msg.chat.id.toString),
        message = Message(data.msg.messageId.toString, data.msg.text),
        callbackData = data.callbackData
      )

  given TelegramMessageToMessageConverter: ObjectConverter[BMessage, Message] =
    (data: BMessage) => Message(data.messageId.toString, data.text)

  given InlineKeyboardToInlineKeyboardMarkup: ObjectConverter[InlineKeyboard, InlineKeyboardMarkup] =
    (inlineKeyboard: InlineKeyboard) => {
      val buttons = inlineKeyboard.buttons.map { row =>
        row.map(createInlineKeyboardButton)
      }
      InlineKeyboardMarkup(buttons)
    }

  private def createInlineKeyboardButton(button: Button) = {
    button match {
      case b: TaggedButton  => InlineKeyboardButton.callbackData(b.label, tag(b.tag))
      case b: LabeledButton => InlineKeyboardButton.callbackData(b.label, b.label)
    }
  }

  private def tag(name: String): String = TagPrefix + name
}
