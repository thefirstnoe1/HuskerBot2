package org.j3y.HuskerBot2.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class BetsPageController {
    @GetMapping("/bets")
    fun betsPageRedirect(): String = "redirect:/bets/index.html"
}