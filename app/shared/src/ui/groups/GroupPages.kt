package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistFab
import com.usuallycoding.opensecretsanta.ui.gift.AskQuestionFab
import com.usuallycoding.opensecretsanta.ui.form.FAB_CLEARANCE
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    if (questions == null) {
        Box(Modifier.fillMaxSize()) {
            PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }, bottomSpace = FAB_CLEARANCE) { WishlistSection(wishlist) }
            WishlistFab(wishlist)
        }
        return
    }
    TabbedPages(
        onShown = { page -> if (page == 0) wishlist.refresh() else questions.refresh() },
    ) { page ->
        if (page == 0) {
            Box(Modifier.fillMaxSize()) {
                PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }, bottomSpace = FAB_CLEARANCE) { WishlistSection(wishlist) }
                WishlistFab(wishlist)
            }
        } else {
            PullPage(loading = questionsState?.loading == true, onRefresh = { questions.refresh() }) { SantaQuestionsSection(questions) }
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
    TabbedPages(
        onShown = { page -> if (page == 0) wishlist.refresh() else questions.refresh() },
    ) { page ->
        if (page == 0) {
            PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }) { ReceiverWishlistSection(wishlist) }
        } else {
            Box(Modifier.fillMaxSize()) {
                PullPage(loading = questionsState.loading, onRefresh = { questions.refresh() }, bottomSpace = FAB_CLEARANCE) { ReceiverQuestionsSection(questions) }
                AskQuestionFab(questions)
            }
        }
    }
}

/**
 * The Wishlist and Questions tabs of the two pages: a tab row and the content of the shown tab, which can be changed by choosing
 * a tab or by swiping the content sideways. [onShown] reloads a tab when it is shown (a tab that was left may be stale).
 */
@Composable
private fun TabbedPages(onShown: (Int) -> Unit, page: @Composable (Int) -> Unit) {
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val latestOnShown by rememberUpdatedState(onShown)
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect { latestOnShown(it) }
    }
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = pager.currentPage) {
            Tab(selected = pager.currentPage == 0, onClick = { scope.launch { pager.animateScrollToPage(0) } }, text = { Text(Strings.TAB_WISHLIST) })
            Tab(selected = pager.currentPage == 1, onClick = { scope.launch { pager.animateScrollToPage(1) } }, text = { Text(Strings.TAB_QUESTIONS) })
        }
        HorizontalPager(pager, Modifier.weight(1f), beyondViewportPageCount = 1) { index -> page(index) }
    }
}
