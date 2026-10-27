package com.usuallycoding.opensecretsanta.server.data.db

import java.sql.Connection
import java.sql.DriverManager

/** Opens SQLite connections to a single database file; callers close the connections they get. */
class Database(private val path: String) {

    fun connection(): Connection =
        DriverManager.getConnection("jdbc:sqlite:$path").also { connection ->
            connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
        }

    /** Highest applied migration version, or 0 when none has been applied. */
    fun schemaVersion(): Int = connection().use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version").use { rows ->
                rows.next()
                rows.getInt(1)
            }
        }
    }
}
