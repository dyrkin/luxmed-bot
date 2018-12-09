
package com.lbs.bot

import com.lbs.bot.model.Event

trait WebhookBot[In <: Event] extends PollBot[In] {
  def processPayload(payload: String, signature: Option[String]): Unit
}
