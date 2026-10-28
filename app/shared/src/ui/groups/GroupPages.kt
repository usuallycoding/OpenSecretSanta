package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverQuestionsSection
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverViewModel
import com.usuallycoding.opensecretsanta.ui.gift.SantaQuestionsSection
import com.usuallycoding.opensecretsanta.ui.gift.SantaQuestionsViewModel
import com.usuallycoding.opensecretsanta.ui.load.PullPage
import com.usuallycoding.opensecretsanta.ui.wishlist.ReceiverWishlistSection
import com.usuallycoding.opensecretsanta.ui.wishlist.ReceiverWishlistViewModel
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistSection
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The page for the relationship in which the person receives: their own wishlist and, once the names are drawn, in a second
 * tab the questions their Santa asked. Nothing on it says who the Santa is.
 */
@Composable
fun ReceiverPage(groupId: Long, wishlist: WishlistViewModel, onBack: () -> Unit) {
    val wishlistState by wishlist.data.collectAsState()
    val phase = wishlistState.value?.phase
    // the questions exist once the names are drawn: the tab, and the request behind it, only then
    val questions = if (phase == GroupPhase.Active || phase == GroupPhase.Archived) {
        koinViewModel<SantaQuestionsViewModel>(key = "santa-questions-$groupId") { parametersOf(groupId) }
    } else {
        null
    }
    val questionsState = questions?.data?.collectAsState()?.value

    if (wishlistState.isGone() || questionsState?.isGone() == true) {
        GroupGone(onBack)
        return
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        if (questions != null) {
            Tabs(tab, onChoose = { chosen ->
                tab = chosen
                // only the shown tab is composed, so a tab that was left may be stale: reload it when it is chosen
                if (chosen == 0) wishlist.refresh() else questions.refresh()
            })
        }
        if (questions == null || tab == 0) {
            PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }, modifier = Modifier.weight(1f)) {
                WishlistSection(wishlist)
            }
        } else {
            PullPage(loading = questionsState?.loading == true, onRefresh = { questions.refresh() }, modifier = Modifier.weight(1f)) {
                SantaQuestionsSection(questions)
            }
        }
    }
}

/**
 * The page for the relationship in which the person gives: what their receiver wants, read-only, in one tab and the
 * person's own questions to the receiver with the answers in the other.
 */
@Composable
fun GifterPage(wishlist: ReceiverWishlistViewModel, questions: ReceiverViewModel, onBack: () -> Unit) {
    val wishlistState by wishlist.state.collectAsState()
    val questionsState by questions.data.collectAsState()

    if (wishlistState.isGone() || questionsState.isGone()) {
        GroupGone(onBack)
        return
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Tabs(tab, onChoose = { chosen ->
            tab = chosen
            if (chosen == 0) wishlist.refresh() else questions.refresh()
        })
        if (tab == 0) {
            PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }, modifier = Modifier.weight(1f)) {
                ReceiverWishlistSection(wishlist)
            }
        } else {
            PullPage(loading = questionsState.loading, onRefresh = { questions.refresh() }, modifier = Modifier.weight(1f)) {
                ReceiverQuestionsSection(questions)
            }
        }
    }
}

/** The Wishlist and Questions tabs of the two pages. */
@Composable
private fun Tabs(selected: Int, onChoose: (Int) -> Unit) {
    PrimaryTabRow(selectedTabIndex = selected) {
        Tab(selected = selected == 0, onClick = { onChoose(0) }, text = { Text(Strings.TAB_WISHLIST) })
        Tab(selected = selected == 1, onClick = { onChoose(1) }, text = { Text(Strings.TAB_QUESTIONS) })
    }
}
