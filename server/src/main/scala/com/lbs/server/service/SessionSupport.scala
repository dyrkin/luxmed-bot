
package com.lbs.server.service

import com.lbs.api.exception.SessionExpiredException
import com.lbs.api.json.model.LoginResponse
import com.lbs.common.{Logger, ParametrizedLock}
import com.lbs.server.ThrowableOr
import com.lbs.server.exception.UserNotFoundException

import scala.collection.mutable

trait SessionSupport extends Logger {

  case class Session(accessToken: String, tokenType: String)

  def login(username: String, password: String): ThrowableOr[LoginResponse]

  protected def dataService: DataService

  private val sessions = mutable.Map[Long, Session]()

  private val lock = new ParametrizedLock[Long]

  protected def withSession[T](accountId: Long)(fn: Session => ThrowableOr[T]): ThrowableOr[T] =
    lock.obtainLock(accountId).synchronized {

      def auth: ThrowableOr[Session] = {
        val credentialsMaybe = dataService.getCredentials(accountId)
        credentialsMaybe match {
          case Some(credentials) =>
            val loginResponse = login(credentials.username, credentials.password)
            loginResponse.map(r => Session(r.accessToken, r.tokenType))
          case None => Left(UserNotFoundException(accountId))
        }
      }

      def getSession: ThrowableOr[Session] = {
        sessions.get(accountId) match {
          case Some(sess) => Right(sess)
          case None =>
            for {
              session <- auth
            } yield {
              sessions.put(accountId, session)
              session
            }
        }
      }

      def doApiCall = {
        for {
          session <- getSession
          result <- fn(session)
        } yield result
      }

      for {
        result <- doApiCall match {
          case Left(_: SessionExpiredException) =>
            debug(s"The session for account [#$accountId] has expired. Try to relogin")
            sessions.remove(accountId)
            doApiCall
          case another =>
            debug(s"Call to remote api function has completed with result:\n$another")
            another
        }
      } yield result
    }

  def addSession(accountId: Long, accessToken: String, tokenType: String): Unit =
    lock.obtainLock(accountId).synchronized {
      sessions.put(accountId, Session(accessToken, tokenType))
    }
}
