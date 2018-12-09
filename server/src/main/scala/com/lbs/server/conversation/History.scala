
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.api.json.model.HistoricVisit
import com.lbs.bot.Bot
import com.lbs.bot.model.Command
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.service.ApiService

class History(val userId: UserId, bot: Bot, apiService: ApiService, val localization: Localization,
              historyPagerFactory: UserIdWithOriginatorTo[Pager[HistoricVisit]])(val actorSystem: ActorSystem) extends Conversation[Unit] with Localizable {

  private val historyPager = historyPagerFactory(userId, self)

  entryPoint(prepareData)

  def prepareData: Step =
    process { _ =>
      val visits = apiService.visitsHistory(userId.accountId)
      historyPager.restart()
      historyPager ! visits
      goto(processResponseFromPager)
    }

  def processResponseFromPager: Step =
    monologue {
      case Msg(cmd: Command, _) =>
        historyPager ! cmd
        stay()
      case Msg(Pager.NoItemsFound, _) =>
        bot.sendMessage(userId.source, lang.visitsHistoryIsEmpty)
        end()
      case Msg(_: HistoricVisit, _) =>
        end()
    }

  beforeDestroy {
    historyPager.destroy()
  }
}