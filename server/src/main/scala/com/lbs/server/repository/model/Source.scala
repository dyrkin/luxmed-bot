package com.lbs.server.repository.model

import jakarta.persistence.*

import scala.beans.BeanProperty
import scala.compiletime.uninitialized

@Entity
@Access(AccessType.FIELD)
class Source extends RecordId {
  @BeanProperty
  @Column(name = "chat_id", nullable = false)
  var chatId: String = uninitialized

  @BeanProperty
  @Column(name = "source_system_id", nullable = false)
  var sourceSystemId: JLong = uninitialized

  @BeanProperty
  @Column(name = "user_id", nullable = false)
  var userId: JLong = uninitialized
}

object Source {
  def apply(chatId: String, sourceSystemId: Long, userId: Long): Source = {
    val source = new Source
    source.chatId = chatId
    source.sourceSystemId = sourceSystemId
    source.userId = userId
    source
  }
}
