
package com.lbs.server.exception

case class UserNotFoundException(chatId: Long) extends Exception(s"Luxmed username for char with id $chatId")
