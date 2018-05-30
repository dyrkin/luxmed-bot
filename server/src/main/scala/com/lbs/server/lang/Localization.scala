/**
 * MIT License
 *
 * Copyright (c) 2018 Yevhen Zadyra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lbs.server.lang

import java.util.concurrent.ConcurrentHashMap

import com.lbs.server.repository.model
import com.lbs.server.service.DataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

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
      case Some(exists) =>
        exists.setLang(lang.id)
        exists
      case None => model.Settings(userId, lang.id)
    }
    dataService.saveSettings(settings)
  }
}
