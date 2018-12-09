
package com.lbs.server

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
class Boot

object Boot extends App {
  SpringApplication.run(classOf[Boot], args: _*)
}
