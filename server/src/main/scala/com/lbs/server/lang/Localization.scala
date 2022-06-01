
package com.lbs.server.lang

import com.lbs.server.repository.model
import com.lbs.server.service.DataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.util.concurrent.ConcurrentHashMap

@Component
class Localization {

  @Autowired
  private var dataService: DataService = _

  private val cachedLangs = new ConcurrentHashMap[Long, Lang]

  def lang(userId: Long): Lang = {
    cachedLangs.computeIfAbsent(userId, _ => {
      val settings = dataService.findSettings(userId)
      settings.map(s => Lang(s.lang)).getOrElse(En)
    })

  }

  def invalidateLangsCache(): Unit = {
    cachedLangs.clear()
  }

  def updateLanguage(userId: Long, lang: Lang): Unit = {
    cachedLangs.put(userId, lang)
    val settings = dataService.findSettings(userId) match {
      case Some(existingSettings) =>
        existingSettings.setLang(lang.id)
        existingSettings
      case None => model.Settings(userId, lang.id, 0, alwaysAskOffset = false)
    }
    dataService.saveSettings(settings)
  }
}
