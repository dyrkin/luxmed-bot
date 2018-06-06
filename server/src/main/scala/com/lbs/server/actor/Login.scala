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
import com.lbs.bot.Bot
import com.lbs.bot.model.{Command, MessageSource}
import com.lbs.bot.telegram.TelegramBot
import com.lbs.server.actor.Chat.Init
import com.lbs.server.actor.Login._
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.service.{ApiService, DataService}
import com.lbs.server.util.MessageExtractors
import org.jasypt.util.text.TextEncryptor

class Login(source: MessageSource, bot: Bot, dataService: DataService, apiService: ApiService, textEncryptor: TextEncryptor, val localization: Localization, originator: ActorRef) extends SafeFSM[FSMState, FSMData] with Localizable {

  protected var userId: UserId = _

  startWith(LogIn, LoginData())

  private var forwardCommand: ForwardCommand = _

  whenSafe(LogIn) {
    case Event(cmd: Command, LoginData(None, None)) =>
      forwardCommand = ForwardCommand(cmd)
      invokeNext()
      goto(RequestUsername)
    case Event(_, LoginData(Some(username), Some(password))) =>
      val loginResult = apiService.login(username, password)
      loginResult match {
        case Left(error) =>
          bot.sendMessage(source, error.getMessage)
          invokeNext()
          goto(RequestUsername) using LoginData()
        case Right(loggedIn) =>
          val credentials = dataService.saveCredentials(source, username, password)
          userId = UserId(credentials.userId, source)
          apiService.addSession(credentials.userId, loggedIn.accessToken, loggedIn.tokenType)
          bot.sendMessage(source, lang.loginAndPasswordAreOk)
          originator ! LoggedIn(forwardCommand, credentials.userId)
          stay() using null
      }
  }

  whenSafe(RequestUsername) {
    case Event(Next, _) =>
      bot.sendMessage(source, lang.provideUsername)
      goto(AwaitUsername)
  }

  whenSafe(AwaitUsername) {
    case Event(Command(_, MessageExtractors.TextOpt(username), _), loginData: LoginData) =>
      invokeNext()
      goto(RequestPassword) using loginData.copy(username = username)
  }

  whenSafe(RequestPassword) {
    case Event(Next, _) =>
      bot.sendMessage(source, lang.providePassword)
      goto(AwaitPassword)
  }

  whenSafe(AwaitPassword) {
    case Event(Command(_, MessageExtractors.TextOpt(password), _), loginData: LoginData) =>
      invokeNext()
      goto(LogIn) using loginData.copy(password = password.map(textEncryptor.encrypt))
  }

  whenUnhandledSafe {
    case Event(Init, _) =>
      goto(LogIn) using LoginData()
    case e: Event =>
      LOG.error(s"Unhandled event in state:$stateName. Event: $e")
      stay()
  }

  initialize()

}

object Login {

  def props(source: MessageSource, bot: Bot, dataService: DataService, apiService: ApiService, textEncryptor: TextEncryptor, localization: Localization, originator: ActorRef): Props =
    Props(new Login(source, bot, dataService, apiService, textEncryptor, localization, originator))

  object LogIn extends FSMState

  object RequestUsername extends FSMState

  object AwaitUsername extends FSMState

  object RequestPassword extends FSMState

  object AwaitPassword extends FSMState

  case class LoginData(username: Option[String] = None, password: Option[String] = None) extends FSMData

  case class ForwardCommand(cmd: Command)

  case class UserId(userId: Long, source: MessageSource)

  case class LoggedIn(forwardCommand: ForwardCommand, userId: Long)

}