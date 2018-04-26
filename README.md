# rest-commander

Spring boot service used to execute commands triggered by REST API endpoint. Some commands are predefined, custom commands can be easily implemented. 

All commands are executed within one transaction.

## Structure and custom commands

Service works with commands - classes that implement `Cmd`. For example:

```kotlin
@Component
@Order(Int.MIN_VALUE)
class SqlScriptCmd(private val jdbcTemplate: JdbcTemplate) : Cmd {

    override fun getName() = "SQL_SCRIPT"

    override fun execute() = runScripts(loadScripts())
       
    private fun runScripts(scripts: List<SqlScript>) {

        scripts.forEach {
            logger.debug("Executing script ${it.name}")
            jdbcTemplate.update(it.sql)
        }
    }

    private fun loadScripts(): List<SqlScript> {

        return PathMatchingResourcePatternResolver()
                .getResources("classpath:/*.sql")             
                .sortedBy { it.filename }
                .map {
                    SqlScript(
                            name = it.filename!!,
                            sql = String(Files.readAllBytes(it.file.toPath()), StandardCharsets.UTF_8))
                }
    }
}
```
As seen above two methods need to be implemented:

- **execute()** - executes command
- **getName()** - defines command name


## Command priorities (order)

Commands are executed in order. Priorities can be configured via `@Ordered` annotation. 

Annotation is optional and represents an order value. 
Lower values have higher priority. The default value is `Ordered.LOWEST_PRECEDENCE`, indicating lowest priority (losing to any other specified order value).

```kotlin
@Component
@Order(1)
class MyFirstCommand : Cmd {

    override fun getName() = "MY_COMMAND_1"

    override fun execute() = TODO() 
}

@Component
@Order(2)
class MySecondCommand : Cmd {

    override fun getName() = "MY_COMMAND_2"

    override fun execute() = TODO() 
}
```

## Including, excluding commands

Let's say multiple commands are implemented. With properties configuration commands can easily be 'activated' or 'deactivated:

- **cmd.includes** - defines list of commands that are _active_. If property is empty all commands are active.
- **cmd.excludes** - defines list of commands that are excluded from execution, therefore _inactive_

In properties all commands are presented using the name of command defined via `getName()` method, for example 'SQL_SCRIPT'

## Predefined commands

Some commands are already implemented and are ready to use.

### Executing SQL scripts

Used to execute SQL scripts (files that end with `.sql`) on classpath. Scripts are executed in alphabetical order.

#### Loading scripts

Scripts on classpath (resources) are loaded by default. Additional scripts location can be added via property `script.path`, for instance:

```
script.path=/Users/nejckorasa/my/path/to/scripts
```

#### Filtering

Filtering of scripts is also supported via properties:

- **script.suffix** - Only runs scripts that match '*${script.suffix}.sql'
- **script.prefix** - Only runs scripts that match '${script.prefix}*.sql'

Suffix and prefix filters can be combined. They can be used to match scripts for specific database (oracle, mssql ...):

```
a-script-oracle.sql
a-script-mssql.sql
b-script-oracle.sql
b-script-mssql.sql

...

```

Using `script.suffix=oracle` would result executing scripts ending with 'oracle'.

#### Including, excluding scripts

Let's say multiple scripts are used. With properties configuration scripts can easily be 'activated' or 'deactivated:

- **script.includes** - defines list of scripts that are _active_. If property is empty all commands are active.
- **script.excludes** - defines list of scripts that are excluded from execution, therefore _inactive_

In properties all scripts are presented using the file name a script, for instance 'script-1-oracle.sql'

#### Reloading (hot swap)

Scripts can either be loaded each time before execution or only once (before first execution). This can be configured via property:

- **script.always-reload**

If set to `true`, scripts can be dynamically added to classpath - no server restart is required!

## Rest API

Commands execution is triggered by REST API endpoint call:

- **POST /execute**

## Swagger API documentation

API documents via swagger UI can be accessed via **/swagger-ui.html**

For instance: [http://localhost:8888/swagger-ui.html](http://localhost:8888/swagger-ui.html) if you run the service locally on port 8888.

## Security

OAuth2 security is supported using JWT (Json Web Tokens). It can be enabled/disabled via spring profiles:

- **no-auth** - No security is enabled
- **oauth2-auth** - OAuth2 Security using JWT (actually [JWK](https://tools.ietf.org/html/rfc7517)) (See property `security.oauth2.resource.jwk.key-set-uri`)

## Setup

- Download ( really? )
- Configure maven or use the bundled version inside a project
- Modify `application.properties` file
- Build project via Maven (`mvn install`)
- Run created jar via (for instance) `java -jar commander-0.0.1-SNAPSHOT.jar`. 
You may want to run jar as a service or at least use `nohup`

