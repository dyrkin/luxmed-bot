
package com.lbs.server.repository.model

import javax.persistence._

import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class Source extends RecordId {
  @BeanProperty
  @Column(name = "chat_id", nullable = false)
  var chatId: String = _

  @BeanProperty
  @Column(name = "source_system_id", nullable = false)
  var sourceSystemId: JLong = _

  @BeanProperty
  @Column(name = "user_id", nullable = false)
  var userId: JLong = _
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