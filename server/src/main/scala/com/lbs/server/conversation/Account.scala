package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot._
import com.lbs.bot.model.Button
import com.lbs.server.conversation.Account._
import com.lbs.server.conversation.Login._
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.service.DataService
import com.lbs.server.util.MessageExtractors.CallbackCommand

class Account(val userId: UserId, bot: Bot, dataService: DataService, val localization: Localization, router: Router)(
  val actorSystem: ActorSystem
) extends Conversation[Unit]
    with Localizable {

  entryPoint(askAction)

  def askAction: Step =
    ask { _ =>
      val credentials = dataService.getUserCredentials(userId.userId)
      val currentAccount =
        credentials.find(c => c.accountId == userId.accountId).getOrElse(sys.error("Can't determine current account"))
      val buttons = Seq(
        Button(lang.addAccount, Tags.AddAccount),
        Button(lang.deleteAccount, Tags.DeleteAccount)
      ) ++ credentials.map(c => Button(s"🔐️ ${c.username}", c.accountId))
      bot.sendMessage(
        userId.source,
        lang.pleaseChooseAccount(currentAccount.username),
        inlineKeyboard = createInlineKeyboard(buttons, columns = 1)
      )
    } onReply { case Msg(cmd @ CallbackCommand(action), _) =>
      action match {
        case Tags.AddAccount =>
          router ! cmd.copy(message = cmd.message.copy(text = Some("/login")), callbackData = None)
        case Tags.DeleteAccount =>
          bot.sendMessage(userId.source, "Not implemented yet")
        case accountId =>
          switchAccount(accountId.toLong)
      }
      end()
    }

  private def switchAccount(accountId: Long): Unit = {
    val accountMaybe = dataService.findUserCredentialsByAccountId(userId.userId, accountId)
    accountMaybe match {
      case Some(account) =>
        val userMaybe = dataService.findUser(userId.userId)
        userMaybe.foreach { user =>
          user.activeAccountId = accountId
          dataService.saveUser(user)
          router ! SwitchAccount(UserId(account.userId, account.accountId, userId.source))
          bot.sendMessage(userId.source, lang.accountSwitched(account.username))
        }
      case None =>
        logger.error(s"This is not user [#${userId.userId}] account [#$accountId]")
    }
  }
}

object Account {

  case class SwitchAccount(userId: UserId)

  object Tags {
    val AddAccount = "add_account"
    val DeleteAccount = "delete_account"
  }

}
