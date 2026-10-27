package com.usuallycoding.opensecretsanta.server.data.db

class Migration(val version: Int, val name: String, val sql: String)

class MigrationException(val version: Int, cause: Throwable) :
    RuntimeException("Migration V$version failed: ${cause.message}", cause)

object ClasspathMigrations {
    private const val DIRECTORY = "db/migration"
    private val FILE_NAME = Regex("""V(\d+)__(.+)\.sql""")

    /**
     * Reads migrations named in the `index.txt` file of [DIRECTORY]. An explicit index is used
     * because scanning the classpath is unreliable once the server is packaged as a jar.
     */
    fun load(loader: ClassLoader = ClasspathMigrations::class.java.classLoader): List<Migration> {
        val index = readResource(loader, "$DIRECTORY/index.txt")
        return index.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { fileName ->
                val match = FILE_NAME.matchEntire(fileName)
                    ?: error("Migration '$fileName' must be named V<number>__<name>.sql")
                Migration(
                    version = match.groupValues[1].toInt(),
                    name = match.groupValues[2],
                    sql = readResource(loader, "$DIRECTORY/$fileName"),
                )
            }
            .toList()
    }

    private fun readResource(loader: ClassLoader, path: String): String =
        loader.getResourceAsStream(path)?.use { it.readBytes().decodeToString() }
            ?: error("Missing classpath resource '$path'")
}
