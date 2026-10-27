package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.domain.user.NewUser
import com.usuallycoding.opensecretsanta.domain.user.Username
import com.usuallycoding.opensecretsanta.server.config.ServerConfig
import com.usuallycoding.opensecretsanta.server.data.db.ClasspathMigrations
import com.usuallycoding.opensecretsanta.server.data.db.Database
import com.usuallycoding.opensecretsanta.server.data.db.MigrationRunner
import com.usuallycoding.opensecretsanta.server.data.user.SqliteUserRepository
import com.usuallycoding.opensecretsanta.server.security.AesGcmSecretBox
import com.usuallycoding.opensecretsanta.server.security.BcryptPasswordHasher
import com.usuallycoding.opensecretsanta.server.security.JwtTokenService
import com.usuallycoding.opensecretsanta.server.security.SystemClock
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

const val TEST_ADMIN_PASSWORD = "initial-admin-pass"
const val TEST_PASSWORD_COST = 4

val TEST_ENCRYPTION_KEY: String = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

fun newTempDbPath(): Path = Files.createTempDirectory("santa-test").resolve("test.db")

fun testConfig(
    dbPath: Path = newTempDbPath(),
    adminUsername: String = "admin",
    adminPassword: String? = TEST_ADMIN_PASSWORD,
) = ServerConfig(
    jwtSecret = "test-secret-with-enough-length-for-hs256",
    dataEncryptionKey = TEST_ENCRYPTION_KEY,
    dbPath = dbPath.toString(),
    port = 0,
    adminUsername = adminUsername,
    adminPassword = adminPassword,
    passwordHashCost = TEST_PASSWORD_COST,
)

fun envWith(vararg extra: Pair<String, String>): Map<String, String> =
    mapOf("JWT_SECRET" to "s3cret", "DATA_ENCRYPTION_KEY" to TEST_ENCRYPTION_KEY) + extra

/**
 * Prepares a migrated database file and helpers to create users and tokens in it before the
 * application under test starts (a non-empty database also keeps the admin seed from running).
 */
class TestFixtures(val config: ServerConfig = testConfig()) {
    val database = Database(config.dbPath).also { MigrationRunner(it, ClasspathMigrations.load()).migrate() }
    val users = SqliteUserRepository(database)
    private val hasher = BcryptPasswordHasher(TEST_PASSWORD_COST)
    private val secretBox = AesGcmSecretBox(config.encryptionKeyBytes())
    private val tokens = JwtTokenService(config.jwtSecret, SystemClock)

    fun addUser(
        name: String,
        password: String = "password1",
        admin: Boolean = false,
        mustChange: Boolean = false,
        tempPlain: String? = null,
    ): Username {
        val username = Username.parse(name)
        users.insertAll(
            listOf(
                NewUser(username, hasher.hash(password), tempPlain?.let(secretBox::encrypt), mustChange, admin),
            ),
        )
        return username
    }

    fun token(name: String): String {
        val username = Username.parse(name)
        return tokens.issue(username, users.find(username)!!.tokenVersion)
    }

    fun sql(statement: String) = database.connection().use { c -> c.createStatement().use { it.execute(statement) } }

    /** Stores Santa to receiver pairs for a group, as activation would have drawn them. */
    fun assign(groupId: Int, vararg pairs: Pair<String, String>) {
        pairs.forEach { (santa, receiver) -> sql("INSERT INTO assignments (group_id, santa, receiver) VALUES ($groupId, '$santa', '$receiver')") }
    }

    /** Creates a group owned by [owner] in [state] with the given participants and returns its id. */
    fun addGroup(owner: String, vararg participants: String, state: String = "pending", name: String = "Group"): Int {
        sql("INSERT INTO groups (owner_username, name, state) VALUES ('$owner', '$name', '$state')")
        val id = database.connection().use { c ->
            c.createStatement().use { s -> s.executeQuery("SELECT MAX(id) FROM groups").use { it.next(); it.getInt(1) } }
        }
        participants.forEachIndexed { index, username ->
            sql("INSERT INTO group_participants (group_id, username, position) VALUES ($id, '$username', $index)")
        }
        return id
    }
}
