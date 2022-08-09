package com.lbs.bot

import com.lbs.bot.model.{Event, InlineKeyboard}

trait PollBot[In <: Event] {
  def sendMessage(chatId: String, text: String): Unit

  def sendMessage(chatId: String, text: String, buttons: Option[InlineKeyboard] = None): Unit

  def sendEditMessage(chatId: String, messageId: String, buttons: Option[InlineKeyboard]): Unit

  def sendEditMessage(chatId: String, messageId: String, text: String, buttons: Option[InlineKeyboard] = None): Unit

  def sendFile(chatId: String, filename: String, contents: Array[Byte], caption: Option[String] = None): Unit

  protected def onReceive(command: In): Unit
}
