package com.usuallycoding.opensecretsanta.contract

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupDtoSerializationTest {
    private val json = Json

    private inline fun <reified T> roundTrip(value: T): String {
        val text = json.encodeToString(value)
        assertEquals(value, json.decodeFromString<T>(text))
        return text
    }

    private val adminGroup = AdminGroupDto(
        id = 7,
        owner = "boss",
        name = "Office party",
        description = "Bring snacks",
        location = "HQ",
        date = "2026-12-24",
        time = "18:30",
        state = "pending",
        participants = listOf(ParticipantDto("alice", hasAccount = true, profileComplete = true), ParticipantDto("bob", false, false)),
    )

    @Test
    fun requestsRoundTrip() {
        roundTrip(GroupDetailsRequest("Party", "d", "HQ", "2026-12-24", "18:30"))
        roundTrip(CreateGroupRequest("Party", participants = listOf("alice", "bob")))
        roundTrip(ParticipantsRequest(listOf("alice")))
    }

    @Test
    fun requestsTolerateMissingOptionalFields() {
        assertEquals(GroupDetailsRequest("Party"), json.decodeFromString<GroupDetailsRequest>("""{"name":"Party"}"""))
        assertEquals(CreateGroupRequest("Party"), json.decodeFromString<CreateGroupRequest>("""{"name":"Party"}"""))
    }

    @Test
    fun responsesRoundTrip() {
        roundTrip(adminGroup)
        roundTrip(GroupChangeResponse(adminGroup, listOf(CreatedAccountDto("bob", "TempPass0001"))))
        roundTrip(GroupChangeResponse(adminGroup))
        roundTrip(MemberGroupDto(7, "Office party", state = "active", participants = listOf("alice", "bob", "carol")))
        roundTrip(AssignmentDto("alice", "bob"))
    }

    @Test
    fun memberViewHasNoAssignmentOrProfileData() {
        val text = roundTrip(MemberGroupDto(7, "Office party", "d", "HQ", "2026-12-24", "18:30", "active", listOf("alice", "bob", "carol")))

        for (forbidden in listOf("santa", "receiver", "assignment", "hasAccount", "profileComplete", "password", "address")) {
            assertFalse(text.contains(forbidden, ignoreCase = true), forbidden)
        }
    }

    @Test
    fun adminGroupNeverCarriesCredentials() {
        val text = roundTrip(adminGroup)

        assertFalse(text.contains("password", ignoreCase = true))
        assertTrue(text.contains("profileComplete"))
    }

    @Test
    fun routeBuildersMatchTheirTemplates() {
        assertEquals("/admin/groups/12", ApiRoutes.adminGroup(12))
        assertEquals("/admin/groups/12/participants", ApiRoutes.adminGroupParticipants(12))
        assertEquals("/admin/groups/12/pending", ApiRoutes.adminGroupPending(12))
        assertEquals("/admin/groups/12/activate", ApiRoutes.adminGroupActivate(12))
        assertEquals("/admin/groups/12/archive", ApiRoutes.adminGroupArchive(12))
        assertEquals("/admin/groups/12/assignments", ApiRoutes.adminGroupAssignments(12))
        assertEquals("/groups/12", ApiRoutes.group(12))
        assertEquals(ApiRoutes.ADMIN_GROUP.replace("{id}", "12"), ApiRoutes.adminGroup(12))
    }
}
