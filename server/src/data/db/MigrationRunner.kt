package com.usuallycoding.opensecretsanta.server.data.db

import java.sql.Connection

class MigrationRunner(
    private val database: Database,
    private val migrations: List<Migration>,
) {
    init {
        val versions = migrations.map { it.version }
        require(versions.size == versions.toSet().size) { "Duplicate migration versions: $versions" }
    }

    /** Applies every migration above the recorded version in ascending order, one transaction each. */
    fun migrate() {
        database.connection().use { connection ->
            connection.autoCommit = false
            connection.createStatement().use {
                it.execute(
                    "CREATE TABLE IF NOT EXISTS schema_version (" +
                        "version INTEGER PRIMARY KEY, " +
                        "applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)",
                )
            }
            connection.commit()

            val applied = currentVersion(connection)
            migrations.filter { it.version > applied }
                .sortedBy { it.version }
                .forEach { apply(connection, it) }
        }
    }

    private fun currentVersion(connection: Connection): Int =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version").use { rows ->
                rows.next()
                rows.getInt(1)
            }
        }

    private fun apply(connection: Connection, migration: Migration) {
        try {
            statementsOf(migration.sql).forEach { sql ->
                connection.createStatement().use { it.execute(sql) }
            }
            connection.prepareStatement("INSERT INTO schema_version (version) VALUES (?)").use {
                it.setInt(1, migration.version)
                it.executeUpdate()
            }
            connection.commit()
        } catch (e: Exception) {
            connection.rollback()
            throw MigrationException(migration.version, e)
        }
    }

    /** Splits a script on semicolons after dropping `--` comment lines; string literals may not contain `;`. */
    private fun statementsOf(script: String): List<String> =
        script.lineSequence()
            .filterNot { it.trim().startsWith("--") }
            .joinToString("\n")
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
