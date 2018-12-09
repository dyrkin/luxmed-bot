
package com.lbs.bot.telegram

import com.lbs.bot.model.Event
import info.mukel.telegrambot4s.models.Message

case class TelegramEvent(msg: Message, callbackData: Option[String]) extends Event
