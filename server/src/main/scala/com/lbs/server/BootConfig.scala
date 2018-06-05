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
import com.lbs.bot.Bot
import com.lbs.bot.model.MessageSource
import com.lbs.bot.telegram.TelegramBot
import com.lbs.server.actor.Login.UserId
import com.lbs.server.actor._
import com.lbs.server.lang.Localization
import com.lbs.server.service.{ApiService, DataService, MonitoringService}
import javax.annotation.PostConstruct
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
  def authActorFactory: MessageSource => ActorRef = source => actorSystem.actorOf(Auth.props(source,
    dataService, unauthorizedHelpActorFactory, loginActorFactory, chatActorFactory))

  @Bean
  def loginActorFactory: (MessageSource, ActorRef) => ActorRef = (source, originator) => actorSystem.actorOf(Login.props(source, bot,
    dataService, apiService, textEncryptor, localization, originator))

  @Bean
  def bookingActorFactory: UserId => ActorRef = userId => actorSystem.actorOf(Book.props(userId, bot, apiService, dataService,
    monitoringService, localization, datePickerFactory, staticDataActorFactory, termsPagerActorFactory))

  @Bean
  def unauthorizedHelpActorFactory: MessageSource => ActorRef = source => actorSystem.actorOf(UnauthorizedHelp.props(source, bot))

  @Bean
  def helpActorFactory: UserId => ActorRef = userId => actorSystem.actorOf(Help.props(userId, bot, localization))

  @Bean
  def monitoringsActorFactory: UserId => ActorRef =
    userId => actorSystem.actorOf(Monitorings.props(userId, bot, monitoringService, localization, monitoringsPagerActorFactory))

  @Bean
  def historyActorFactory: UserId => ActorRef =
    userId => actorSystem.actorOf(History.props(userId, bot, apiService, localization, historyPagerActorFactory))

  @Bean
  def visitsActorFactory: UserId => ActorRef =
    userId => actorSystem.actorOf(Visits.props(userId, bot, apiService, localization, visitsPagerActorFactory))

  @Bean
  def bugActorFactory: UserId => ActorRef =
    userId => actorSystem.actorOf(Bug.props(userId, bot, dataService, bugPagerActorFactory, localization))

  @Bean
  def settingsActorFactory: UserId => ActorRef =
    userId => actorSystem.actorOf(Settings.props(userId, bot, dataService, localization))

  @Bean
  def chatActorFactory: UserId => ActorRef =
    userId => actorSystem.actorOf(Chat.props(userId, dataService, monitoringService, bookingActorFactory, helpActorFactory,
      monitoringsActorFactory, historyActorFactory, visitsActorFactory, settingsActorFactory, bugActorFactory))

  @Bean
  def datePickerFactory: (UserId, ActorRef) => ActorRef = (userId, originator) =>
    actorSystem.actorOf(DatePicker.props(userId, bot, localization, originator))

  @Bean
  def staticDataActorFactory: (UserId, ActorRef) => ActorRef = (userId, originator) =>
    actorSystem.actorOf(StaticData.props(userId, bot, localization, originator))

  @Bean
  def termsPagerActorFactory: (UserId, ActorRef) => ActorRef = (userId, originator) =>
    actorSystem.actorOf(Pagers(userId, bot, localization).termsPagerProps(originator))

  @Bean
  def visitsPagerActorFactory: (UserId, ActorRef) => ActorRef = (userId, originator) =>
    actorSystem.actorOf(Pagers(userId, bot, localization).visitsPagerProps(originator))

  @Bean
  def bugPagerActorFactory: (UserId, ActorRef) => ActorRef = (userId, originator) =>
    actorSystem.actorOf(Pagers(userId, bot, localization).bugPagerProps(originator))

  @Bean
  def historyPagerActorFactory: (UserId, ActorRef) => ActorRef = (userId, originator) =>
    actorSystem.actorOf(Pagers(userId, bot, localization).historyPagerProps(originator))

  @Bean
  def monitoringsPagerActorFactory: (UserId, ActorRef) => ActorRef = (userId, originator) =>
    actorSystem.actorOf(Pagers(userId, bot, localization).monitoringsPagerProps(originator))

  @Bean
  def router: ActorRef = actorSystem.actorOf(Router.props(authActorFactory))

  @Bean
  def telegram: TelegramBot = new TelegramBot(router ! _, telegramBotToken)

  @Bean
  def bot: Bot = new Bot(telegram)
}
