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
import com.lbs.common.{Logger, ParametrizedLock}
import com.lbs.server.exception.UserNotFoundException

import scala.collection.mutable

trait SessionSupport extends Logger {

  case class Session(accessToken: String, tokenType: String)

  def login(username: String, password: String): Either[Throwable, LoginResponse]

  protected def dataService: DataService

  private val sessions = mutable.Map[Long, Session]()

  private val lock = new ParametrizedLock[Long]

  protected def withSession[T](accountId: Long)(fn: Session => Either[Throwable, T]): Either[Throwable, T] =
    lock.obtainLock(accountId).synchronized {

      def auth: Either[Throwable, Session] = {
        val credentialsMaybe = dataService.getCredentials(accountId)
        credentialsMaybe match {
          case Some(credentials) =>
            val loginResponse = login(credentials.username, credentials.password)
            loginResponse.map(r => Session(r.accessToken, r.tokenType))
          case None => Left(UserNotFoundException(accountId))
        }
      }

      def session: Either[Throwable, Session] = {
        sessions.get(accountId) match {
          case Some(sess) => Right(sess)
          case None =>
            auth match {
              case Right(sess) =>
                sessions.put(accountId, sess)
                Right(sess)
              case left => left
            }
        }
      }

      session match {
        case Right(s) =>
          fn(s) match {
            case Left(ex) if ex.getMessage.contains("session has expired") =>
              debug(s"The session for account [#$accountId] has expired. Try to relogin")
              sessions.remove(accountId)
              session.flatMap(fn)
            case another =>
              debug(s"Call to remote api function has completed with result:\n$another")
              another
          }
        case Left(ex) => Left(ex)
      }
    }

  def addSession(accountId: Long, accessToken: String, tokenType: String): Unit =
    lock.obtainLock(accountId).synchronized {
      sessions.put(accountId, Session(accessToken, tokenType))
    }
}
