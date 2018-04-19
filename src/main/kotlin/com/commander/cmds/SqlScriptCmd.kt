package com.commander.cmds

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.nio.file.Files

private data class SqlScript(val name: String, val sql: String)

/**
 * @author Nejc Korasa
 */

@Component
@Order(Int.MIN_VALUE)
class SqlScriptCmd(
        @Value("\${script.excludes:}") private val scriptExcludesConfValue: String,
        @Value("\${script.includes:}") private val scriptIncludesConfValue: String,
        @Value("\${script.prefix:}") private val prefix: String,
        @Value("\${script.suffix:}") private val suffix: String,
        @Value("\${script.always-reload:false}") private val alwaysReload: Boolean,
        private val jdbcTemplate: JdbcTemplate) : Cmd {

    private val scriptExcludes: List<String> by lazy {
        if (scriptExcludesConfValue.isBlank()) emptyList()
        else scriptExcludesConfValue.split(",").toList()
    }

    private val scriptIncludes: List<String> by lazy {
        if (scriptIncludesConfValue.isBlank()) emptyList()
        else scriptIncludesConfValue.split(",").toList()
    }

    private val logger = LoggerFactory.getLogger(SqlScriptCmd::class.java)
    private var scripts = listOf<SqlScript>()


    override fun getName() = "SQL_SCRIPT"

    override fun execute() {

        if (alwaysReload) {
            logger.debug("Reloading scripts")
            runScripts(loadScripts())
        } else {
            if (scripts.isEmpty()) {
                logger.debug("Loading scripts")
                scripts = loadScripts()
            }
            runScripts(scripts)
        }
    }

    private fun runScripts(scripts: List<SqlScript>) = scripts.forEach {
        logger.debug("Executing script ${it.name}")
        jdbcTemplate.update(it.sql)
    }

    private fun loadScripts(): List<SqlScript> {

        return PathMatchingResourcePatternResolver()
                .getResources("classpath:/$prefix*$suffix.sql")
                .filter { scriptIncludes.isEmpty() || scriptExcludes.contains(it.filename) }
                .filter { !scriptExcludes.contains(it.filename) }
                .sortedBy { it.filename }
                .map {
                    SqlScript(
                            name = it.filename!!,
                            sql = String(Files.readAllBytes(it.file.toPath()), StandardCharsets.UTF_8))
                }
    }
}