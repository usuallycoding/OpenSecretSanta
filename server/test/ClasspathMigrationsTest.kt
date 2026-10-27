package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ClasspathMigrationsTest {

    @Test
    fun bundledMigrationsAreOrderedAndStartWithBaseline() {
        val migrations = ClasspathMigrations.load()
        assertEquals(listOf(1), migrations.map { it.version })
        assertEquals("baseline", migrations.first().name)
    }

    @Test
    fun schemaVersionReadsBackAsOneAfterBoot() {
        val path = Files.createTempDirectory("santa-db").resolve("boot.db")
        val database = Database(path.toString())

        MigrationRunner(database, ClasspathMigrations.load()).migrate()

        assertEquals(1, database.schemaVersion())
    }
}
