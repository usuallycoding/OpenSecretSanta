package com.usuallycoding.opensecretsanta.server

import com.usuallycoding.opensecretsanta.server.di.serverModules
import com.usuallycoding.opensecretsanta.domain.group.ActivateGroup
import com.usuallycoding.opensecretsanta.domain.group.ArchiveGroup
import com.usuallycoding.opensecretsanta.domain.group.AssignmentPlanner
import com.usuallycoding.opensecretsanta.domain.group.CreateGroup
import com.usuallycoding.opensecretsanta.domain.group.DeleteDraftGroup
import com.usuallycoding.opensecretsanta.domain.group.GetMyGroup
import com.usuallycoding.opensecretsanta.domain.group.GetOwnedGroup
import com.usuallycoding.opensecretsanta.domain.group.GroupRepository
import com.usuallycoding.opensecretsanta.domain.group.ListAssignments
import com.usuallycoding.opensecretsanta.domain.group.ListMyGroups
import com.usuallycoding.opensecretsanta.domain.group.ListOwnedGroups
import com.usuallycoding.opensecretsanta.domain.group.MoveToPending
import com.usuallycoding.opensecretsanta.domain.group.SetParticipants
import com.usuallycoding.opensecretsanta.domain.group.Shuffler
import com.usuallycoding.opensecretsanta.domain.group.UpdateGroupDetails
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun moduleGraphIsComplete() {
        val config = testConfig()
        module { includes(serverModules(config, migrations = emptyList())) }.verify()
    }

    @Test
    fun everyGroupComponentCanBeResolvedFromAStartedContainer() {
        val app = koinApplication { modules(serverModules(testConfig(), migrations = emptyList())) }
        try {
            val koin = app.koin
            val resolved = listOf(
                koin.get<GroupRepository>(), koin.get<Shuffler>(), koin.get<AssignmentPlanner>(),
                koin.get<CreateGroup>(), koin.get<UpdateGroupDetails>(), koin.get<DeleteDraftGroup>(),
                koin.get<ListOwnedGroups>(), koin.get<GetOwnedGroup>(), koin.get<SetParticipants>(),
                koin.get<MoveToPending>(), koin.get<ActivateGroup>(), koin.get<ArchiveGroup>(),
                koin.get<ListAssignments>(), koin.get<ListMyGroups>(), koin.get<GetMyGroup>(),
            )

            assertEquals(15, resolved.size)
            assertTrue(resolved.all { it != null })
        } finally {
            app.close()
        }
    }
}
