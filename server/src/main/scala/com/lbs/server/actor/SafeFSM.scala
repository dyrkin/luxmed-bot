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

import akka.actor.FSM
import com.lbs.common.Logger

trait SafeFSM[S, D] extends FSM[S, D] with Logger {

  protected val defaultEventHandler: StateFunction = {
    case e: Event =>
      warn(s"Unhandled event in state:$stateName. Event: $e")
      stay()
  }

  protected var eventHandler: StateFunction = defaultEventHandler

  protected def whenSafe(state: S)(stateFunction: StateFunction): Unit = {
    when(state) {
      case event: Event =>
        try {
          if (stateFunction.isDefinedAt(event)) stateFunction(event)
          else eventHandler(event)
        } catch {
          case e: Exception =>
            error(s"Exception occurred while processing event $event", e)
            stay()
        }
    }
  }

  protected def whenUnhandledSafe(stateFunction: StateFunction): Unit = {
    whenUnhandled(stateFunction)
    eventHandler = stateFunction orElse defaultEventHandler
  }
}
