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
package com.lbs.server.service

import com.lbs.api.json.model.LoginResponse
import com.lbs.common.ParametrizedLock
import com.lbs.server.exception.UserNotFoundException
import org.slf4j.LoggerFactory

import scala.collection.mutable

trait SessionSupport {

  private val Log = LoggerFactory.getLogger(classOf[SessionSupport])

  case class Session(accessToken: String, tokenType: String)

  def login(username: String, password: String): Either[Throwable, LoginResponse]

  protected def dataService: DataService

  private val sessions = mutable.Map[Long, Session]()

  private val lock = new ParametrizedLock[Long]

  protected def withSession[T](userId: Long)(fn: Session => Either[Throwable, T]): Either[Throwable, T] =
    lock.obtainLock(userId).synchronized {

      def auth: Either[Throwable, Session] = {
        val credentialsMaybe = dataService.getCredentials(userId)
        credentialsMaybe match {
          case Some(credentials) =>
            val loginResponse = login(credentials.username, credentials.password)
            loginResponse.map(r => Session(r.accessToken, r.tokenType))
          case None => Left(UserNotFoundException(userId))
        }
      }

      def session: Either[Throwable, Session] = {
        sessions.get(userId) match {
          case Some(sess) => Right(sess)
          case None =>
            auth match {
              case Right(sess) =>
                sessions.put(userId, sess)
                Right(sess)
              case left => left
            }
        }
      }

      session match {
        case Right(s) =>
          fn(s) match {
            case Left(ex) if ex.getMessage.contains("session has expired") =>
              Log.debug(s"The session for user with chat id: $userId has expired. Try to relogin")
              sessions.remove(userId)
              session.flatMap(fn)
            case another =>
              Log.debug(s"Call to remote api function has completed with result:\n$another")
              another
          }
        case Left(ex) => Left(ex)
      }
    }

  def addSession(userId: Long, accessToken: String, tokenType: String): Unit =
    lock.obtainLock(userId).synchronized {
      sessions.put(userId, Session(accessToken, tokenType))
    }
}
