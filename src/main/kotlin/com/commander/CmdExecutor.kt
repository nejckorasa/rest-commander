package com.commander

import com.commander.cmds.Cmd
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.annotation.PostConstruct


/**
 * @author Nejc Korasa
 */

@Component
class CmdExecutor(
        @Value("\${cmd.excludes:}") private val cmdExcludesConfValue: String,
        @Value("\${cmd.includes:}") private val cmdIncludesConfValue: String,
        private val cmds: List<Cmd>) {

    private val cmdExcludes: List<String> by lazy {
        if (cmdExcludesConfValue.isBlank()) emptyList()
        else cmdExcludesConfValue.split(",").toList()
    }

    private val cmdIncludes: List<String> by lazy {
        if (cmdIncludesConfValue.isBlank()) emptyList()
        else cmdIncludesConfValue.split(",").toList()
    }

    private val logger = LoggerFactory.getLogger(CmdExecutor::class.java)


    @PostConstruct
    fun init() = logger.info("Registered (ordered) commands -> ${cmds.joinToString(",") { it.javaClass.simpleName }}")

    @Transactional
    fun execute() {

        cmds
                .filter { cmdIncludes.isEmpty() || cmdIncludes.contains(it.getName()) }
                .filter { !cmdExcludes.contains(it.getName()) }
                .forEach {
                    try {
                        logger.debug("Executing command on ${it.javaClass.simpleName}")
                        it.execute()
                    } catch (e: Exception) {
                        logger.error("Error while executing command on ${it.javaClass.simpleName}", e)
                        throw e
                    }
                }
    }
}