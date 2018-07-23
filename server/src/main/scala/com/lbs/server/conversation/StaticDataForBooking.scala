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
