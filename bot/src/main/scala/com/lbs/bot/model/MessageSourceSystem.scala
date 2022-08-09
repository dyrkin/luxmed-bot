package com.lbs.bot.model

trait MessageSourceSystem {
  def id: Long

  def name: String

  override def toString: String = name
}

object MessageSourceSystem {
  val MessageSourceSystems: Seq[MessageSourceSystem] = Seq(TelegramMessageSourceSystem, FacebookMessageSourceSystem)

  private val MessageSourceSystemsMap = MessageSourceSystems.map(e => e.id -> e).toMap

  def apply(id: Long): MessageSourceSystem = {
    MessageSourceSystemsMap.getOrElse(id, sys.error(s"Unsupported source system $id"))
  }
}

object TelegramMessageSourceSystem extends MessageSourceSystem {
  override def id: Long = 1

  override def name: String = "Telegram"
}

object FacebookMessageSourceSystem extends MessageSourceSystem {
  override def id: Long = 2

  override def name: String = "Facebook"
}
