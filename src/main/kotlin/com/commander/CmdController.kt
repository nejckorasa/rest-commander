package com.commander

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * @author Nejc Korasa
 */

@RestController
class CmdController(private val cmdExecutor: CmdExecutor) {

    @PostMapping("execute")
    fun execute() = cmdExecutor.execute()
}