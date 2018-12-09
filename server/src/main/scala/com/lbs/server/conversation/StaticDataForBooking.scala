
package com.lbs.server.conversation

import com.lbs.api.json.model.IdName
import com.lbs.bot.model.Command
import com.lbs.server.conversation.Book.BookingData
import com.lbs.server.conversation.StaticData.{FindOptions, FoundOptions, LatestOptions, StaticDataConfig}
import com.lbs.server.conversation.base.Conversation

trait StaticDataForBooking extends Conversation[BookingData] {

  private[conversation] def staticData: StaticData

  protected def withFunctions(latestOptions: => Seq[IdName], staticOptions: => Either[Throwable, List[IdName]], applyId: IdName => BookingData): Step => MessageProcessorFn = {
    nextStep: Step => {
      case Msg(cmd: Command, _) =>
        staticData ! cmd
        stay()
      case Msg(LatestOptions, _) =>
        staticData ! LatestOptions(latestOptions)
        stay()
      case Msg(FindOptions(searchText), _) =>
        staticData ! FoundOptions(filterOptions(staticOptions, searchText))
        stay()
      case Msg(id: IdName, _) =>
        goto(nextStep) using applyId(id)
    }
  }

  protected def staticData(staticDataConfig: => StaticDataConfig)(functions: BookingData => Step => MessageProcessorFn)(requestNext: Step)(implicit functionName: sourcecode.Name): Step = {
    ask { _ =>
      staticData.restart()
      staticData ! staticDataConfig
    } onReply {
      case msg@Msg(_, bookingData: BookingData) =>
        val fn = functions(bookingData)(requestNext)
        fn(msg)
    }
  }

  private def filterOptions(options: Either[Throwable, List[IdName]], searchText: String) = {
    options.map(opt => opt.filter(c => c.name.toLowerCase.contains(searchText)))
  }
}
