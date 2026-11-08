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

class AnnouncementsMigrationTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }

    private fun count(): Int = database.connection().use { c ->
        c.createStatement().use { s -> s.executeQuery("SELECT COUNT(*) FROM announcements").use { it.next(); it.getInt(1) } }
    }

    @Test
    fun theAnnouncementsTableHangsOffTheGroup() {
        val columns = database.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT name FROM pragma_table_info('announcements') ORDER BY cid").use { r ->
                    generateSequence { if (r.next()) r.getString(1) else null }.toList()
                }
            }
        }

        assertEquals(listOf("group_id", "number", "title", "body", "sent_at"), columns)
    }

    @Test
    fun deletingAGroupDeletesItsAnnouncements() {
        val boss = Username.parse("boss")
        SqliteUserRepository(database).insertAll(listOf(NewUser(boss, "h", null, false, true)))
        val groups = SqliteGroupRepository(database)
        val id = groups.create(boss, GroupDetails("Party"), emptyList()).id
        database.connection().use { c ->
            c.createStatement().use { s -> s.execute("INSERT INTO announcements (group_id, number, title, body, sent_at) VALUES ($id, 1, 't', 'b', 100)") }
        }
        assertEquals(1, count())

        groups.delete(id)

        assertEquals(0, count())
    }
}
