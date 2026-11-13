package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.notification.DevicePlatform
import com.usuallycoding.opensecretsanta.domain.notification.NotificationCategory
import com.usuallycoding.opensecretsanta.domain.notification.NotificationSettings
import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.notification.SqliteDeviceRepository
import com.usuallycoding.opensecretsanta.server.data.notification.SqliteDigestMarkRepository
import com.usuallycoding.opensecretsanta.server.data.notification.SqliteNotificationSettingsRepository
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SqliteNotificationRepositoriesTest {
    private val database = Database(newTempDbPath().toString()).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }
    private val devices = SqliteDeviceRepository(database)
    private val settings = SqliteNotificationSettingsRepository(database)

    private val alice = Username.parse("alice")
    private val bob = Username.parse("bob")

    init {
        SqliteUserRepository(database).insertAll(listOf(alice, bob).map { NewUser(it, "h", null, false, false) })
    }

    private fun tokensOf(vararg names: Username) = devices.devicesOf(names.toList()).map { it.token }

    @Test
    fun aPersonCanHaveSeveralDevices() {
        devices.register(alice, "t1", DevicePlatform.ANDROID, 1)
        devices.register(alice, "t2", DevicePlatform.WEB, 2)

        assertEquals(listOf("t1", "t2"), tokensOf(alice))
        assertEquals(listOf(DevicePlatform.ANDROID, DevicePlatform.WEB), devices.devicesOf(listOf(alice)).map { it.platform })
    }

    @Test
    fun theSameTokenTwiceIsOneDevice() {
        devices.register(alice, "t1", DevicePlatform.ANDROID, 1)
        devices.register(alice, "t1", DevicePlatform.ANDROID, 2)

        assertEquals(listOf("t1"), tokensOf(alice))
    }

    @Test
    fun aTokenMovesToTheNextPersonWhoRegistersIt() {
        devices.register(alice, "t1", DevicePlatform.ANDROID, 1)
        devices.register(bob, "t1", DevicePlatform.ANDROID, 2)

        assertEquals(emptyList(), tokensOf(alice))
        assertEquals(listOf("t1"), tokensOf(bob))
    }

    @Test
    fun removingOnlyRemovesYourOwnToken() {
        devices.register(alice, "t1", DevicePlatform.ANDROID, 1)
        devices.register(bob, "t2", DevicePlatform.WEB, 1)

        devices.remove(bob, "t1")
        assertEquals(listOf("t1"), tokensOf(alice))

        devices.remove(alice, "t1")
        assertEquals(emptyList(), tokensOf(alice))
        assertEquals(listOf("t2"), tokensOf(bob))
    }

    @Test
    fun invalidTokensGoWhoeverOwnsThem() {
        devices.register(alice, "t1", DevicePlatform.ANDROID, 1)
        devices.register(bob, "t2", DevicePlatform.WEB, 1)
        devices.register(bob, "t3", DevicePlatform.WEB, 1)

        devices.removeInvalid(listOf("t1", "t3", "unknown"))

        assertEquals(emptyList(), tokensOf(alice))
        assertEquals(listOf("t2"), tokensOf(bob))
    }

    @Test
    fun lookingUpNobodyFindsNothing() {
        assertEquals(emptyList(), devices.devicesOf(emptyList()))
        assertEquals(emptyMap(), settings.findAll(emptyList()))
    }

    @Test
    fun everythingIsOnUntilChanged() {
        assertEquals(NotificationSettings(), settings.find(alice))
        assertTrue(NotificationCategory.entries.all { settings.find(alice).allows(it) })
    }

    @Test
    fun oneSwitchCanBeTurnedOffWithoutTouchingTheOther() {
        settings.save(alice, NotificationSettings(daily = false))

        val saved = settings.find(alice)
        assertFalse(saved.allows(NotificationCategory.DAILY))
        assertTrue(saved.allows(NotificationCategory.ANNOUNCEMENTS))
        assertEquals(NotificationSettings(), settings.find(bob))
    }

    @Test
    fun savingAgainReplacesTheChoices() {
        settings.save(alice, NotificationSettings(announcements = false, daily = false))
        settings.save(alice, NotificationSettings(announcements = true, daily = false))

        assertEquals(NotificationSettings(daily = false), settings.find(alice))
    }

    @Test
    fun theFourOlderColumnsFollowTheDigestSwitch() {
        settings.save(alice, NotificationSettings(daily = false))

        val older = database.connection().use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT wishlist, questions, answers, letters FROM notification_prefs WHERE username = 'alice'").use {
                    it.next()
                    (1..4).map(it::getInt)
                }
            }
        }
        assertEquals(listOf(0, 0, 0, 0), older)
    }

    @Test
    fun aMarkIsKeptOncePerPersonAndTakingThemAllClearsThem() {
        val marks = SqliteDigestMarkRepository(database)
        marks.mark(bob, 5)
        marks.mark(alice, 7)
        marks.mark(bob, 9)

        assertEquals(listOf(bob, alice), marks.takeAll())
        assertEquals(emptyList(), marks.takeAll())
    }

    @Test
    fun severalPeopleAreReadTogetherWithDefaultsForTheRest() {
        settings.save(alice, NotificationSettings(announcements = false))

        val all = settings.findAll(listOf(alice, bob))

        assertEquals(NotificationSettings(announcements = false), all[alice])
        assertEquals(NotificationSettings(), all[bob])
    }
}
