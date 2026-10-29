package com.usuallycoding.opensecretsanta.ui

import com.usuallycoding.opensecretsanta.ui.groups.GroupListFilter
import com.usuallycoding.opensecretsanta.ui.groups.GroupListItem
import com.usuallycoding.opensecretsanta.ui.groups.adminStateChips
import com.usuallycoding.opensecretsanta.ui.groups.participantStateChips
import com.usuallycoding.opensecretsanta.ui.groups.filterGroups
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupFilterTest {
    private data class Item(
        override val name: String,
        override val description: String? = null,
        override val stateKey: String = "active",
        val location: String? = null,
    ) : GroupListItem

    private val office = Item("Office Party", "Bring snacks", "pending", location = "Town Hall")
    private val book = Item("Book Club", null, "active")
    private val reverse = Item("Party at the Office", "Drinks only", "archived")
    private val draft = Item("Planning", "Office ideas", "draft")
    private val all = listOf(office, book, reverse, draft)

    private fun names(query: String = "", vararg states: String, items: List<Item> = all) =
        filterGroups(items, query, states.toSet()).map { it.name }

    // --- what the search matches ---

    @Test
    fun theNameIsSearched() {
        assertEquals(listOf("Office Party"), names("party", items = listOf(office, book)))
    }

    @Test
    fun theDescriptionIsSearched() {
        assertEquals(listOf("Office Party"), names("snacks"))
    }

    @Test
    fun caseIsIgnoredInTheTextAndInTheQuery() {
        assertEquals(listOf("Office Party", "Party at the Office", "Planning"), names("OFFICE"))
        assertEquals(listOf("Book Club"), names("bOoK"))
    }

    @Test
    fun everyWordMustBeFoundInTheNameOrTheDescription() {
        assertEquals(listOf("Office Party"), names("party snacks"))
        assertEquals(emptyList(), names("party snacks pizza"))
    }

    @Test
    fun wordsMayBeInAnyOrderAndInEitherField() {
        assertEquals(listOf("Office Party", "Party at the Office"), names("office party"))
        assertEquals(listOf("Office Party"), names("snacks party"))
    }

    @Test
    fun aWordIsNotFoundAcrossTheEndOfTheNameAndTheStartOfTheDescription() {
        assertEquals(emptyList(), names("clubbring", items = listOf(Item("Book Club", "Bring snacks"))))
    }

    @Test
    fun otherDetailsAreNotSearched() {
        assertEquals(emptyList(), names("town hall"))
    }

    @Test
    fun aMissingDescriptionCountsAsEmpty() {
        assertEquals(listOf("Book Club"), names("club"))
        assertEquals(emptyList(), names("null"))
    }

    @Test
    fun aBlankQueryShowsEveryGroup() {
        assertEquals(all.map { it.name }, names(""))
        assertEquals(all.map { it.name }, names("   \t "))
    }

    @Test
    fun extraSpacesBetweenAndAroundWordsAreIgnored() {
        assertEquals(listOf("Office Party"), names("   party    snacks  "))
    }

    // --- chips ---

    @Test
    fun noChipShowsEveryState() {
        assertEquals(all.map { it.name }, names())
    }

    @Test
    fun oneChipShowsOnlyThatState() {
        assertEquals(listOf("Book Club"), names("", "active"))
    }

    @Test
    fun severalChipsShowAnyOfTheirStates() {
        assertEquals(listOf("Office Party", "Book Club"), names("", "pending", "active"))
    }

    @Test
    fun aGroupInAStateWithoutAChipIsShownOnlyWhileNoChipIsOn() {
        val frozen = Item("Frozen group", stateKey = "frozen")
        assertEquals(listOf("Frozen group"), names(items = listOf(frozen)))
        assertEquals(emptyList(), names("", "active", "pending", items = listOf(frozen)))
    }

    // --- together ---

    @Test
    fun theSearchAndTheChipsBothNarrowTheList() {
        assertEquals(listOf("Party at the Office"), names("party", "archived"))
        assertEquals(emptyList(), names("party", "active"))
    }

    @Test
    fun theOrderTheServerGaveIsKept() {
        val ordered = listOf(reverse, office, draft, book)
        assertEquals(listOf("Party at the Office", "Office Party", "Planning"), names("office", items = ordered))
        assertEquals(listOf("Party at the Office", "Office Party"), names("office", "pending", "archived", items = ordered))
    }

    @Test
    fun theSameListComesBackWhenNothingIsAsked() {
        assertEquals(all, filterGroups(all, "", emptySet()))
    }
    // --- the state holder ---

    @Test
    fun theChipsOfTheTwoListsAreTheStatesEachMayHave() {
        assertEquals(listOf("pending", "active", "archived"), participantStateChips.map { it.key })
        assertEquals(listOf("Pending", "Active", "Archived"), participantStateChips.map { it.label })
        assertEquals(listOf("draft", "pending", "active", "archived"), adminStateChips.map { it.key })
        assertEquals(listOf("Draft", "Pending", "Active", "Archived"), adminStateChips.map { it.label })
    }

    @Test
    fun theFilterStartsEmpty() {
        val filter = GroupListFilter(adminStateChips)

        assertEquals("", filter.query)
        assertEquals(emptySet(), filter.states)
        assertEquals(all, filter.apply(all))
    }

    @Test
    fun severalChipsCanBeOnAndOneCanBeSwitchedOffAgain() {
        val filter = GroupListFilter(adminStateChips)

        filter.toggle("pending")
        filter.toggle("active")
        assertEquals(setOf("pending", "active"), filter.states)

        filter.toggle("pending")
        assertEquals(setOf("active"), filter.states)

        filter.toggle("active")
        assertEquals(emptySet(), filter.states)
    }

    @Test
    fun aStateWithoutAChipCannotBeSwitchedOn() {
        val filter = GroupListFilter(participantStateChips)

        filter.toggle("draft")
        filter.toggle("nonsense")

        assertEquals(emptySet(), filter.states)
    }

    @Test
    fun theFilterAppliesTheQueryAndTheChipsTogether() {
        val filter = GroupListFilter(adminStateChips)
        filter.setQuery("office")
        filter.toggle("pending")
        filter.toggle("draft")

        assertEquals(listOf("Office Party", "Planning"), filter.apply(all).map { it.name })
    }

    @Test
    fun clearingEmptiesTheSearchAndSwitchesEveryChipOff() {
        val filter = GroupListFilter(adminStateChips)
        filter.setQuery("party")
        filter.toggle("active")
        filter.toggle("archived")

        filter.clear()

        assertEquals("", filter.query)
        assertEquals(emptySet(), filter.states)
        assertEquals(all, filter.apply(all))
    }

    @Test
    fun aQueryOfOnlySpacesIsKeptAsTypedButFiltersNothing() {
        val filter = GroupListFilter(adminStateChips)

        filter.setQuery("   ")

        assertEquals("   ", filter.query)
        assertEquals(all, filter.apply(all))
    }

}
