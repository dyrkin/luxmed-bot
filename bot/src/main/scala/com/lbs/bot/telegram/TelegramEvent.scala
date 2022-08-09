package com.lbs.bot.telegram

import com.bot4s.telegram.models.Message
import com.lbs.bot.model.Event

case class TelegramEvent(msg: Message, callbackData: Option[String]) extends Event
