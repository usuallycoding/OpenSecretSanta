package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** What the search and the state chips need to know about a group in one of the two lists. */
interface GroupListItem {
    val name: String
    val description: String?

    /** The state as the server names it, in lower case: `draft`, `pending`, `active` or `archived`. */
    val stateKey: String
}

/**
 * The groups of [items] that match [query] and are in one of [states], in the order they came in.
 *
 * A group matches the query when every word of it (split at spaces, ignoring case) is found in the group's name or
 * description, in any order; an empty or blank query matches every group. A group's date, place and participants are not
 * searched. With no [states] every state is allowed; with some, a group is kept only when its state is one of them, so a
 * group in a state that has no chip is shown only while no chip is on.
 */
fun <T : GroupListItem> filterGroups(items: List<T>, query: String, states: Set<String>): List<T> {
    val words = query.lowercase().split(WHITESPACE).filter { it.isNotEmpty() }
    if (words.isEmpty() && states.isEmpty()) return items
    return items.filter { item ->
        (states.isEmpty() || item.stateKey in states) && matches(item, words)
    }
}

private val WHITESPACE = Regex("\\s+")

private fun matches(item: GroupListItem, words: List<String>): Boolean {
    if (words.isEmpty()) return true
    // a line break between the two, so that a word never matches across the end of one and the start of the other
    val text = (item.name + "\n" + item.description.orEmpty()).lowercase()
    return words.all { it in text }
}

/** One state chip: the state as the server names it, and what the chip says. */
data class StateChip(val key: String, val label: String)

private fun chipsFor(vararg states: String) = states.map { StateChip(it, groupStateInfo(it).label) }

/** The chips of a participant's list: a participant is never shown a Draft. */
val participantStateChips: List<StateChip> = chipsFor("pending", "active", "archived")

/** The chips of an administrator's list, which has every state. */
val adminStateChips: List<StateChip> = chipsFor("draft", "pending", "active", "archived")

/**
 * What the person has typed into the search field and which state chips are on, for one list. It is kept by the list's view
 * model, so it outlives a reload and a visit to another screen, and goes when the person signs out. The groups themselves
 * are not kept here: the screen applies it to whatever the list has loaded.
 *
 * The query and the chips are Compose state, not flows: a text field that keeps its own copy of what is typed takes the
 * text it is given back after every keystroke, and a flow delivers it a frame late, so on a slow frame the next letters
 * typed meanwhile were taken away again. State read in the same frame is never behind.
 */
class GroupListFilter(val chips: List<StateChip>) {
    private var typed by mutableStateOf("")
    private var chosen by mutableStateOf<Set<String>>(emptySet())

    /** What is typed, exactly as typed (it may hold only spaces). */
    val query: String get() = typed

    /** The keys of the chips that are on; empty means every state. */
    val states: Set<String> get() = chosen

    fun setQuery(text: String) {
        typed = text
    }

    /** Switches the chip of [key] on if it is off and off if it is on; a key that has no chip changes nothing. */
    fun toggle(key: String) {
        if (chips.none { it.key == key }) return
        chosen = if (key in chosen) chosen - key else chosen + key
    }

    /** Empties the search and switches every chip off. */
    fun clear() {
        typed = ""
        chosen = emptySet()
    }

    /** [items] narrowed by what is typed and which chips are on, in their order. */
    fun <T : GroupListItem> apply(items: List<T>): List<T> = filterGroups(items, query, states)
}
