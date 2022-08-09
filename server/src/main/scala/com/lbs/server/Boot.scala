
package com.lbs.server

import com.typesafe.scalalogging.StrictLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
class Boot

object Boot extends App with StrictLogging {

  def printInfo(): Unit = {
    logger.info(s"Num processors: ${Runtime.getRuntime.availableProcessors}")
  }

  printInfo()
  SpringApplication.run(classOf[Boot], args: _*)
}
