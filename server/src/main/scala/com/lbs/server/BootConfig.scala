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
package com.lbs.server

import akka.actor.{ActorRef, ActorSystem}
import com.lbs.api.json.model.{AvailableVisitsTermPresentation, HistoricVisit, ReservedVisit}
import com.lbs.bot.Bot
import com.lbs.bot.telegram.TelegramBot
import com.lbs.server.conversation._
import com.lbs.server.lang.Localization
import com.lbs.server.repository.model
import com.lbs.server.repository.model.Monitoring
import com.lbs.server.service.{ApiService, DataService, MonitoringService}
import org.jasypt.util.text.{StrongTextEncryptor, TextEncryptor}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.context.annotation.{Bean, Configuration}

@Configuration
class BootConfig {
  @Value("${security.secret}")
  private var secret: String = _

  @Value("${telegram.token}")
  private var telegramBotToken: String = _

  @Autowired
  private var apiService: ApiService = _

  @Autowired
  private var dataService: DataService = _

  @Autowired
  private var monitoringService: MonitoringService = _

  @Autowired
  private var localization: Localization = _

  @Bean
  def actorSystem = ActorSystem()

  @Bean
  def textEncryptor: TextEncryptor = {
    val encryptor = new StrongTextEncryptor
    encryptor.setPassword(secret)
    encryptor
  }

  @Bean
  def authFactory: MessageSourceTo[Auth] = source => new Auth(source,
    dataService, unauthorizedHelpFactory, loginFactory, chatFactory)(actorSystem)

  @Bean
  def loginFactory: MessageSourceWithOriginatorTo[Login] = (source, originator) => new Login(source, bot,
    dataService, apiService, textEncryptor, localization, originator)(actorSystem)

  @Bean
  def bookFactory: UserIdTo[Book] = userId => new Book(userId, bot, apiService, dataService,
    monitoringService, localization, datePickerFactory, timePickerFactory, staticDataFactory, termsPagerFactory)(actorSystem)

  @Bean
  def unauthorizedHelpFactory: MessageSourceTo[UnauthorizedHelp] = source => new UnauthorizedHelp(source, bot)(actorSystem)

  @Bean
  def helpFactory: UserIdTo[Help] = userId => new Help(userId, bot, localization)(actorSystem)

  @Bean
  def monitoringsFactory: UserIdTo[Monitorings] =
    userId => new Monitorings(userId, bot, monitoringService, localization, monitoringsPagerFactory)(actorSystem)

  @Bean
  def historyFactory: UserIdTo[History] =
    userId => new History(userId, bot, apiService, localization, historyPagerFactory)(actorSystem)

  @Bean
  def visitsFactory: UserIdTo[Visits] =
    userId => new Visits(userId, bot, apiService, localization, visitsPagerFactory)(actorSystem)

  @Bean
  def bugFactory: UserIdTo[Bug] =
    userId => new Bug(userId, bot, dataService, bugPagerFactory, localization)(actorSystem)

  @Bean
  def settingsFactory: UserIdTo[Settings] =
    userId => new Settings(userId, bot, dataService, localization)(actorSystem)

  @Bean
  def accountFactory: UserIdTo[Account] =
    userId => new Account(userId, bot, dataService, localization, router)(actorSystem)

  @Bean
  def chatFactory: UserIdTo[Chat] =
    userId => new Chat(userId, dataService, monitoringService, bookFactory, helpFactory,
      monitoringsFactory, historyFactory, visitsFactory, settingsFactory, bugFactory, accountFactory)(actorSystem)

  @Bean
  def datePickerFactory: UserIdWithOriginatorTo[DatePicker] = (userId, originator) =>
    new DatePicker(userId, bot, localization, originator)(actorSystem)

  @Bean
  def timePickerFactory: UserIdWithOriginatorTo[TimePicker] = (userId, originator) =>
    new TimePicker(userId, bot, localization, originator)(actorSystem)

  @Bean
  def staticDataFactory: UserIdWithOriginatorTo[StaticData] = (userId, originator) =>
    new StaticData(userId, bot, localization, originator)(actorSystem)

  @Bean
  def termsPagerFactory: UserIdWithOriginatorTo[Pager[AvailableVisitsTermPresentation]] = (userId, originator) =>
    new Pager[AvailableVisitsTermPresentation](userId, bot,
      (term: AvailableVisitsTermPresentation, page: Int, index: Int) => lang(userId).termEntry(term, page, index),
      (page: Int, pages: Int) => lang(userId).termsHeader(page, pages),
      Some("book"), localization, originator)(actorSystem)


  @Bean
  def visitsPagerFactory: UserIdWithOriginatorTo[Pager[ReservedVisit]] = (userId, originator) =>
    new Pager[ReservedVisit](userId, bot,
      (visit: ReservedVisit, page: Int, index: Int) => lang(userId).upcomingVisitEntry(visit, page, index),
      (page: Int, pages: Int) => lang(userId).upcomingVisitsHeader(page, pages),
      Some("cancel"), localization, originator)(actorSystem)

  @Bean
  def bugPagerFactory: UserIdWithOriginatorTo[Pager[model.Bug]] = (userId, originator) =>
    new Pager[model.Bug](userId, bot,
      (bug: model.Bug, page: Int, index: Int) => lang(userId).bugEntry(bug, page, index),
      (page: Int, pages: Int) => lang(userId).bugsHeader(page, pages),
      None, localization, originator)(actorSystem)

  @Bean
  def historyPagerFactory: UserIdWithOriginatorTo[Pager[HistoricVisit]] = (userId, originator) =>
    new Pager[HistoricVisit](userId, bot,
      (visit: HistoricVisit, page: Int, index: Int) => lang(userId).historyEntry(visit, page, index),
      (page: Int, pages: Int) => lang(userId).historyHeader(page, pages),
      Some("repeat"), localization, originator)(actorSystem)

  @Bean
  def monitoringsPagerFactory: UserIdWithOriginatorTo[Pager[Monitoring]] = (userId, originator) =>
    new Pager[Monitoring](userId, bot,
      (monitoring: Monitoring, page: Int, index: Int) => lang(userId).monitoringEntry(monitoring, page, index),
      (page: Int, pages: Int) => lang(userId).monitoringsHeader(page, pages),
      Some("cancel"), localization, originator)(actorSystem)

  @Bean
  def router: ActorRef = actorSystem.actorOf(Router.props(authFactory))

  @Bean
  def telegram: TelegramBot = new TelegramBot(router ! _, telegramBotToken)

  @Bean
  def bot: Bot = new Bot(telegram)

  private def lang(userId: Login.UserId) = {
    localization.lang(userId.userId)
  }
}
