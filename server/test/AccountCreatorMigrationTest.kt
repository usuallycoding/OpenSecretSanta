package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountCreatorMigrationTest {
    /** A database as it was before V008, with these accounts, and then migrated to the end. */
    private fun migrated(vararg accounts: Pair<String, Boolean>): Database {
        val database = Database(newTempDbPath().toString())
        val all = ClasspathMigrations.load()
        MigrationRunner(database, all.filter { it.version < 8 }).migrate()
        database.connection().use { c ->
            c.createStatement().use { s ->
                accounts.forEach { (name, admin) ->
                    s.execute("INSERT INTO users (username, password_hash, is_admin) VALUES ('$name', 'h', ${if (admin) 1 else 0})")
                }
            }
        }
        MigrationRunner(database, all).migrate()
        return database
    }

    private fun creators(database: Database): Map<String, String?> = database.connection().use { c ->
        c.createStatement().use { s ->
            s.executeQuery("SELECT username, created_by FROM users").use { rows ->
                generateSequence { if (rows.next()) rows.getString(1) to rows.getString(2) else null }.toMap()
            }
        }
    }

    @Test
    fun withOneAdministratorEveryOtherAccountIsTheirs() {
        val database = migrated("boss" to true, "alice" to false, "bob" to false, "carol" to false)

        assertEquals(mapOf("boss" to null, "alice" to "boss", "bob" to "boss", "carol" to "boss"), creators(database))
    }

    @Test
    fun withSeveralAdministratorsNoAccountIsGivenACreator() {
        val database = migrated("boss" to true, "second" to true, "alice" to false)

        assertEquals(mapOf("boss" to null, "second" to null, "alice" to null), creators(database))
    }

    @Test
    fun anAccountMadeAfterwardsStartsWithoutACreatorUntilTheAppSetsOne() {
        val database = migrated("boss" to true)
        database.connection().use { c -> c.createStatement().use { it.execute("INSERT INTO users (username, password_hash) VALUES ('late', 'h')") } }

        assertEquals(null, creators(database)["late"])
    }
}
