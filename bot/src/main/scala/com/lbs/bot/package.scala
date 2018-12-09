
package com.lbs

import com.lbs.bot.model.{Button, InlineKeyboard}

package object bot {
  def createInlineKeyboard(buttons: Seq[Button], columns: Int = 2): Option[InlineKeyboard] = {
    Option(buttons).filterNot(_.isEmpty).map(b => InlineKeyboard(b.grouped(columns).toSeq))
  }

  def createInlineKeyboard(buttons: Seq[Seq[Button]]): Option[InlineKeyboard] = {
    Option(buttons).filterNot(_.isEmpty).map(InlineKeyboard)
  }
}
