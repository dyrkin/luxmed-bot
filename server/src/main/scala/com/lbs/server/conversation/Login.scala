
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot.Bot
import com.lbs.bot.model.{Command, MessageSource}
import com.lbs.server.conversation.Login._
import com.lbs.server.conversation.base.{Conversation, Interactional}
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.service.{ApiService, DataService}
import com.lbs.server.util.MessageExtractors
import org.jasypt.util.text.TextEncryptor

class Login(source: MessageSource, bot: Bot, dataService: DataService, apiService: ApiService, textEncryptor: TextEncryptor,
            val localization: Localization, originator: Interactional)(val actorSystem: ActorSystem) extends Conversation[String] with Localizable {

  protected var userId: UserId = _

  entryPoint(logIn)

  private var forwardCommand: ForwardCommand = _

  def logIn: Step =
    monologue {
      case Msg(cmd: Command, _) =>
        forwardCommand = ForwardCommand(cmd)
        goto(requestUsername)
    }

  def requestUsername: Step =
    ask { _ =>
      bot.sendMessage(source, lang.provideUsername)
    } onReply {
      case Msg(MessageExtractors.TextCommand(username), _) =>
        goto(requestPassword) using username
    }

  def requestPassword: Step =
    ask { _ =>
      bot.sendMessage(source, lang.providePassword)
    } onReply {
      case Msg(MessageExtractors.TextCommand(plainPassword), username) =>
        val password = textEncryptor.encrypt(plainPassword)
        val loginResult = apiService.login(username, password)
        loginResult match {
          case Left(error) =>
            bot.sendMessage(source, error.getMessage)
            goto(requestUsername)
          case Right(loggedIn) =>
            val credentials = dataService.saveCredentials(source, username, password)
            userId = UserId(credentials.userId, credentials.accountId, source)
            apiService.addSession(credentials.accountId, loggedIn.accessToken, loggedIn.tokenType)
            bot.sendMessage(source, lang.loginAndPasswordAreOk)
            originator ! LoggedIn(forwardCommand, credentials.userId, credentials.accountId)
            end()
        }
    }
}

object Login {

  case class ForwardCommand(cmd: Command)

  case class UserId(userId: Long, accountId: Long, source: MessageSource)

  case class LoggedIn(forwardCommand: ForwardCommand, userId: Long, accountId: Long)

}