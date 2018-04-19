package com.commander.cmds

/**
 * @author Nejc Korasa
 */

interface Cmd {

    fun execute()

    fun getName(): String
}