
package com.lbs.server

import com.lbs.common.Logger
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
class Boot

object Boot extends App with Logger {

  def printInfo(): Unit = {
    info(s"Num processors: ${Runtime.getRuntime.availableProcessors}")
  }

  printInfo()
  SpringApplication.run(classOf[Boot], args: _*)
}
