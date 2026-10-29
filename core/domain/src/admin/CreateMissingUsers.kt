package com.usuallycoding.opensecretsanta.domain.admin

import com.usuallycoding.opensecretsanta.domain.auth.PasswordHasher
import com.usuallycoding.opensecretsanta.domain.auth.SecretBox
import com.usuallycoding.opensecretsanta.domain.auth.TempPasswordGenerator
import com.usuallycoding.opensecretsanta.domain.user.UserRepository
import com.usuallycoding.opensecretsanta.domain.user.Username

/**
 * Creates an account, with a generated temporary password, for each username that does not exist yet.
 * Existing users are left alone, and a single invalid entry means nothing is created.
 */
class CreateMissingUsers(
    private val users: UserRepository,
    private val planner: NewAccountPlanner,
) {
    constructor(
        users: UserRepository,
        hasher: PasswordHasher,
        secretBox: SecretBox,
        generator: TempPasswordGenerator,
    ) : this(users, NewAccountPlanner(hasher, secretBox, generator))

    operator fun invoke(rawUsernames: List<String>, createdBy: Username? = null): List<TemporaryCredentials> {
        val requested = rawUsernames.map { Username.parse(it, field = "usernames") }.distinct()
        val planned = planner.plan(requested, existing = users.existing(requested), createdBy = createdBy)

        users.insertAll(planned.map { it.newUser })
        return planned.map { TemporaryCredentials(it.username, it.temporaryPassword) }
    }
}
