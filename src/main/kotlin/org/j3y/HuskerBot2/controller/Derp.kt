package org.j3y.HuskerBot2.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class Derp {

    @GetMapping("/derp")
    fun derp(): String {
        return "HELLO!"
    }
}