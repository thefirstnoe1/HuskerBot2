package org.j3y.HuskerBot2

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.retry.annotation.EnableRetry

@SpringBootApplication
@EnableScheduling
@EnableRetry
class HuskerBot2Application

fun main(args: Array<String>) {
	runApplication<HuskerBot2Application>(*args)
}
