package com.commander.cmds

import org.aspectj.weaver.tools.cache.SimpleCacheFactory.path
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.annotation.PostConstruct
import javax.sql.DataSource

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
        @Value("\${script.path:}") private val scriptsPath: String,
        @Value("\${script.always-reload:false}") private val alwaysReload: Boolean,
        private val datasource: DataSource,
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

    @PostConstruct
    fun init() {

        if (!alwaysReload) {
            scripts = loadScripts()
            when {
                logger.isDebugEnabled -> logger.debug("Loaded scripts: ${scripts.asSequence().joinToString(",")}")
                else -> logger.info("Loaded scripts: ${scripts.asSequence().map { it.name }.joinToString(",")}")
            }
        }
    }


    override fun getName() = "SQL_SCRIPT"

    override fun execute() = when {
        alwaysReload -> runScripts(loadScripts())
        else -> runScripts(scripts)
    }

    private fun runScripts(scripts: List<SqlScript>) = scripts.forEach { script ->
        logger.debug("Executing script ${script.name}")
        val statements = mutableListOf<String>().also { ScriptUtils.splitSqlScript(script.sql, ";", it) }
        statements.forEach { jdbcTemplate.update(it) }
    }

    private fun loadScripts(): List<SqlScript> {

        val scripts = PathMatchingResourcePatternResolver()
                .getResources("classpath:/$prefix*$suffix.sql")
                .filter { scriptIncludes.isEmpty() || scriptExcludes.contains(it.filename) }
                .filter { !scriptExcludes.contains(it.filename) }
                .sortedBy { it.filename }
                .map { SqlScript(name = it.filename!!, sql = readInputStream(it.inputStream))
                }.toMutableList()

        if (scriptsPath.isNotBlank()) {

            val locationPattern = "file:/$path/$prefix*$suffix.sql"
            logger.debug("Loading scripts from $locationPattern")
            PathMatchingResourcePatternResolver()
                    .getResources("file:/$path/$prefix*$suffix.sql")
                    .filter { scriptIncludes.isEmpty() || scriptExcludes.contains(it.filename) }
                    .filter { !scriptExcludes.contains(it.filename) }
                    .sortedBy { it.filename }
                    .map { SqlScript(name = it.filename!!, sql = readInputStream(it.inputStream)) }
                    .let { scripts.addAll(it) }
        }

        return scripts
    }

    private fun readInputStream(inputStream: InputStream): String = StringBuilder().also {

        val isr = InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name()))
        BufferedReader(isr).use { reader ->
            var c: Int
            while (true) {
                c = reader.read()
                if (c == -1) break
                it.append(c.toChar())
            }
        }
    }.toString()
}