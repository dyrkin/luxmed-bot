
package com.lbs.server.lang

import com.lbs.server.conversation.Login.UserId

trait Localizable {
  protected def userId: UserId

  protected def localization: Localization

  protected def lang: Lang = Option(userId).map(uId => localization.lang(uId.userId)).getOrElse(En)
}
