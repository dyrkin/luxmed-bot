
package com.lbs.server.repository.model

import javax.persistence.{Access, AccessType, Column, Entity}

import scala.beans.BeanProperty

@Entity
@Access(AccessType.FIELD)
class Settings extends RecordId {
  @BeanProperty
  @Column(name = "user_id", unique = true, nullable = false)
  var userId: JLong = _

  @BeanProperty
  @Column(nullable = false)
  var lang: Int = 0 //En by default
}

object Settings {
  def apply(userId: Long, lang: Int): Settings = {
    val settings = new Settings
    settings.userId = userId
    settings.lang = lang
    settings
  }
}