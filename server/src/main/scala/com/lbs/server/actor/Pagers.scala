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
package com.lbs.server.actor

import akka.actor.{ActorRef, Props}
import com.lbs.api.json.model.{AvailableVisitsTermPresentation, HistoricVisit, ReservedVisit}
import com.lbs.bot.Bot
import com.lbs.server.actor.Login.UserId
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model
import com.lbs.server.repository.model.Monitoring

class Pagers(val userId: UserId, bot: Bot, val localization: Localization) extends Localizable {

  def termsPagerProps(originator: ActorRef): Props =
    Pager.props[AvailableVisitsTermPresentation](userId, bot,
      (term: AvailableVisitsTermPresentation, page: Int, index: Int) => lang.termEntry(term, page, index),
      (page: Int, pages: Int) => lang.termsHeader(page, pages),
      Some("book"), localization, originator)

  def historyPagerProps(originator: ActorRef): Props =
    Pager.props[HistoricVisit](userId, bot,
      (visit: HistoricVisit, page: Int, index: Int) => lang.historyEntry(visit, page, index),
      (page: Int, pages: Int) => lang.historyHeader(page, pages),
      Some("repeat"), localization, originator)

  def visitsPagerProps(originator: ActorRef): Props =
    Pager.props[ReservedVisit](userId, bot,
      (visit: ReservedVisit, page: Int, index: Int) => lang.upcomingVisitEntry(visit, page, index),
      (page: Int, pages: Int) => lang.upcomingVisitsHeader(page, pages),
      Some("cancel"), localization, originator)

  def bugPagerProps(originator: ActorRef): Props =
    Pager.props[model.Bug](userId, bot,
      (bug: model.Bug, page: Int, index: Int) => lang.bugEntry(bug, page, index),
      (page: Int, pages: Int) => lang.bugsHeader(page, pages),
      None, localization, originator)

  def monitoringsPagerProps(originator: ActorRef): Props =
    Pager.props[Monitoring](userId, bot,
      (monitoring: Monitoring, page: Int, index: Int) => lang.monitoringEntry(monitoring, page, index),
      (page: Int, pages: Int) => lang.monitoringsHeader(page, pages),
      Some("cancel"), localization, originator)

}

object Pagers {
  def apply(userId: UserId, bot: Bot, localization: Localization) = new Pagers(userId, bot, localization)
}