package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.group.GroupDetails
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.group.SqliteGroupRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class AnnouncementReadsMigrationTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }

    private fun count(): Int = database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM announcement_reads").use { it.next(); it.getInt(1) } }
    }

    @Test
    fun theReadsTableIsKeyedByGroupAndPerson() {
        val columns = database.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT name FROM pragma_table_info('announcement_reads') ORDER BY cid").use { r ->
                    generateSequence { if (r.next()) r.getString(1) else null }.toList()
                }
            }
        }

        assertEquals(listOf("group_id", "username", "seen_up_to"), columns)
    }

    @Test
    fun deletingAGroupDeletesItsMarks() {
        val boss = Username.parse("boss")
        val users = SqliteUserRepository(database)
        users.insertAll(listOf(NewUser(boss, "h", null, false, true), NewUser(Username.parse("alice"), "h", null, false, false)))
        val groups = SqliteGroupRepository(database)
        val id = groups.create(boss, GroupDetails("Party"), listOf(Username.parse("alice"))).id
        database.connection().use { c ->
            c.createStatement().use { s -> s.execute("INSERT INTO announcement_reads (group_id, username, seen_up_to) VALUES ($id, 'alice', 100)") }
        }
        assertEquals(1, count())

        groups.delete(id)

        assertEquals(0, count())
    }
}
