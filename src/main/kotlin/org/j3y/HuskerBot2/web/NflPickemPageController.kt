package org.j3y.HuskerBot2.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class NflPickemPageController {
    @GetMapping("/pickem/nfl")
    fun nflPickemPageRedirect(): String = "redirect:/pickem/nfl/index.html"
}
