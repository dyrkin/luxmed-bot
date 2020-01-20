
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.bot._
import com.lbs.bot.model.Command
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Pager.ItemsProvider
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.repository.model.Monitoring
import com.lbs.server.service.MonitoringService

class MonitoringsHistory(val userId: UserId, bot: Bot, monitoringService: MonitoringService, val localization: Localization, monitoringsPagerFactory: UserIdWithOriginatorTo[Pager[Monitoring]], bookWithTemplateFactory: UserIdTo[BookWithTemplate])(val actorSystem: ActorSystem) extends Conversation[Monitoring] with Localizable {

  private val monitoringsPager = monitoringsPagerFactory(userId, self)
  private val bookWithTemplate = bookWithTemplateFactory(userId)

  entryPoint(prepareData)

  def prepareData: Step =
    process { _ =>
      val monitoringsCount = monitoringService.getAllMonitoringsCount(userId.accountId)
      monitoringsPager.restart()
      monitoringsPager ! Right(new MonitoringItemsProvider(monitoringsCount.toInt))
      goto(processResponseFromPager)
    }

  def processResponseFromPager: Step =
    monologue {
      case Msg(cmd: Command, _) =>
        monitoringsPager ! cmd
        stay()
      case Msg(Pager.NoItemsFound, _) =>
        bot.sendMessage(userId.source, lang.noActiveMonitorings)
        end()
      case Msg(monitoring: Monitoring, _) =>
        goto(askRepeatMonitoring) using monitoring
    }

  def askRepeatMonitoring: Step =
    ask { monitoring =>
      bookWithTemplate.restart()
      bookWithTemplate ! monitoring
    } onReply {
      case Msg(cmd: Command, _) =>
        bookWithTemplate ! cmd
        stay()
    }

  beforeDestroy {
    monitoringsPager.destroy()
    bookWithTemplate.destroy()
  }

  class MonitoringItemsProvider(val itemsCount: Int) extends ItemsProvider[Monitoring] {
    private var index: Int = 0

    override def pages: Int = itemsCount / Pager.PageSize + 1

    override def next(): Unit = index += 1

    override def previous(): Unit = index -= 1

    override def items: Seq[Monitoring] = monitoringService.getMonitoringsPage(userId.accountId, index * Pager.PageSize, Pager.PageSize)

    override def currentPage: Int = index

    override def isEmpty: Boolean = itemsCount == 0
  }

}

