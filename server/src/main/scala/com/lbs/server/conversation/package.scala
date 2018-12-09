
package com.lbs.server

import com.lbs.bot.model.MessageSource
import com.lbs.server.conversation.Login.UserId
import com.lbs.server.conversation.base.Interactional

package object conversation {
  type UserIdWithOriginatorTo[T <: Interactional] = (UserId, Interactional) => T
  type UserIdTo[T <: Interactional] = UserId => T
  type MessageSourceTo[T <: Interactional] = MessageSource => T
  type MessageSourceWithOriginatorTo[T <: Interactional] = (MessageSource, Interactional) => T
}
