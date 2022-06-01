
package com.lbs.server.conversation

import akka.actor.ActorSystem
import com.lbs.api.json.model.Event
import com.lbs.bot.model.{Button, Command}
import com.lbs.bot.{Bot, _}
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.Pager.SimpleItemsProvider
import com.lbs.server.conversation.ReservedVisitsViewer.Tags
import com.lbs.server.conversation.base.Conversation
import com.lbs.server.lang.{Localizable, Localization}
import com.lbs.server.service.ApiService

class ReservedVisitsViewer(val userId: UserId, bot: Bot, apiService: ApiService, val localization: Localization,
                           visitsPagerFactory: UserIdWithOriginatorTo[Pager[Event]])(val actorSystem: ActorSystem) extends Conversation[Event] with Localizable {

  private val reservedVisitsPager = visitsPagerFactory(userId, self)

  entryPoint(prepareData)

  def prepareData: Step =
    process { _ =>
      val visits = apiService.reserved(userId.accountId)
      reservedVisitsPager.restart()
      reservedVisitsPager ! visits.map(new SimpleItemsProvider(_))
      goto(processResponseFromPager)
    }

  def processResponseFromPager: Step =
    monologue {
      case Msg(cmd: Command, _) =>
        reservedVisitsPager ! cmd
        stay()
      case Msg(Pager.NoItemsFound, _) =>
        bot.sendMessage(userId.source, lang.noUpcomingVisits)
        end()
      case Msg(visit: Event, _) =>
        goto(askToCancelVisit) using visit
    }

  def askToCancelVisit: Step =
    ask { visit =>
      bot.sendMessage(userId.source, lang.areYouSureToCancelAppointment(visit),
        inlineKeyboard = createInlineKeyboard(Seq(Button(lang.no, Tags.No), Button(lang.yes, Tags.Yes))))
    } onReply {
      case Msg(Command(_, _, Some(Tags.No)), _) =>
        bot.sendMessage(userId.source, lang.appointmentWasNotCancelled)
        end()
      case Msg(Command(_, _, Some(Tags.Yes)), visit: Event) =>
        apiService.deleteReservation(userId.accountId, visit.eventId) match {
          case Left(ex) => bot.sendMessage(userId.source, lang.unableToCancelUpcomingVisit(ex.getMessage))
          case Right(_) => bot.sendMessage(userId.source, lang.appointmentHasBeenCancelled)
        }
        end()
    }

  beforeDestroy {
    reservedVisitsPager.destroy()
  }
}

object ReservedVisitsViewer {

  object Tags {
    val Yes = "yes"
    val No = "no"
  }

}