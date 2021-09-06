package co.`fun`.joker.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/joker2021")
class SimpleController {
    @GetMapping("simple")
    suspend fun get(): String {
        return "Just simple response =)"
    }
}