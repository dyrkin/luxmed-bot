package com.lbs.server

import com.typesafe.scalalogging.StrictLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Boot

object Boot extends StrictLogging {

  def printInfo(): Unit = {
    logger.info(s"Num processors: ${Runtime.getRuntime.availableProcessors}")
  }

  @main def run(): Unit = {
    printInfo()
    SpringApplication.run(classOf[Boot], Array[String]()*)
  }
}
