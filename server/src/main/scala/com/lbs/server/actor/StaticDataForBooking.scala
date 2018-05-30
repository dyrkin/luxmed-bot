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

import akka.actor.ActorRef
import com.lbs.api.json.model.IdName
import com.lbs.bot.model.Command
import com.lbs.server.actor.Book.BookingData
import com.lbs.server.actor.StaticData.{FindOptions, FoundOptions, LatestOptions, StaticDataConfig}

trait StaticDataForBooking extends SafeFSM[FSMState, FSMData] {

  protected def staticData: ActorRef

  protected def withFunctions(latestOptions: => Seq[IdName], staticOptions: => Either[Throwable, List[IdName]], applyId: IdName => BookingData): FSMState => StateFunction = {
    nextState: FSMState => {
      case Event(cmd: Command, _) =>
        staticData ! cmd
        stay()
      case Event(LatestOptions, _) =>
        staticData ! LatestOptions(latestOptions)
        stay()
      case Event(FindOptions(searchText), _) =>
        staticData ! FoundOptions(filterOptions(staticOptions, searchText))
        stay()
      case Event(id: IdName, _) =>
        invokeNext()
        goto(nextState) using applyId(id)
    }
  }

  protected def requestStaticData(requestState: FSMState, awaitState: FSMState, staticDataConfig: => StaticDataConfig)(functions: BookingData => FSMState => StateFunction)(requestNext: FSMState): Unit = {
    whenSafe(requestState) {
      case Event(_, _) =>
        staticData ! staticDataConfig
        goto(awaitState)
    }
    whenSafe(awaitState) {
      case event@Event(_, bookingData: BookingData) =>
        val fn = functions(bookingData)(requestNext)
        if (fn.isDefinedAt(event)) fn(event) else eventHandler(event)
    }
  }

  private def filterOptions(options: Either[Throwable, List[IdName]], searchText: String) = {
    options.map(opt => opt.filter(c => c.name.toLowerCase.contains(searchText)))
  }
}
