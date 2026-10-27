package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.Migration
import com.usuallycoding.opensecretsanta.server.data.db.MigrationException
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MigrationRunnerTest {

    private fun newDatabasePath(): Path =
        Files.createTempDirectory("santa-db").resolve("test.db").also { it.toFile().deleteOnExit() }

    private val createItems = Migration(1, "items", "CREATE TABLE items (id INTEGER PRIMARY KEY, name TEXT NOT NULL);")
    private val createOwners = Migration(2, "owners", "CREATE TABLE owners (id INTEGER PRIMARY KEY);")

    private fun Database.count(table: String): Int = connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM $table").use { it.next(); it.getInt(1) } }
    }

    private fun Database.hasTable(table: String): Boolean = connection().use { c ->
        c.createStatement().use { s ->
            s.executeQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '$table'").use { it.next() }
        }
    }

    @Test
    fun freshDatabaseGetsFileAndAllMigrations() {
        val path = newDatabasePath()
        assertFalse(Files.exists(path))
        val database = Database(path.toString())

        MigrationRunner(database, listOf(createOwners, createItems)).migrate()

        assertTrue(Files.exists(path))
        assertTrue(database.hasTable("items"))
        assertTrue(database.hasTable("owners"))
        assertEquals(2, database.schemaVersion())
    }

    @Test
    fun rerunIsIdempotentAndKeepsData() {
        val database = Database(newDatabasePath().toString())
        val runner = MigrationRunner(database, listOf(createItems, createOwners))
        runner.migrate()
        database.connection().use { it.createStatement().use { s -> s.execute("INSERT INTO items (name) VALUES ('kept')") } }

        runner.migrate()

        assertEquals(1, database.count("items"))
        assertEquals(2, database.count("schema_version"))
        assertEquals(2, database.schemaVersion())
    }

    @Test
    fun newMigrationsAreAppliedOnLaterStart() {
        val database = Database(newDatabasePath().toString())
        MigrationRunner(database, listOf(createItems)).migrate()
        assertEquals(1, database.schemaVersion())

        MigrationRunner(database, listOf(createItems, createOwners)).migrate()

        assertEquals(2, database.schemaVersion())
        assertTrue(database.hasTable("owners"))
    }

    @Test
    fun failingMigrationIsRolledBackAndNotRecorded() {
        val database = Database(newDatabasePath().toString())
        val broken = Migration(2, "broken", "CREATE TABLE half_done (id INTEGER); INSERT INTO missing_table VALUES (1);")

        val error = assertFailsWith<MigrationException> {
            MigrationRunner(database, listOf(createItems, broken)).migrate()
        }

        assertEquals(2, error.version)
        assertEquals(1, database.schemaVersion())
        assertFalse(database.hasTable("half_done"))
        assertTrue(database.hasTable("items"))
    }
}
