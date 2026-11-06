package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistFab
import com.usuallycoding.opensecretsanta.ui.gift.AskQuestionFab
import com.usuallycoding.opensecretsanta.ui.form.fabClearance
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverQuestionsSection
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverViewModel
import com.usuallycoding.opensecretsanta.ui.gift.SantaQuestionsSection
import com.usuallycoding.opensecretsanta.ui.gift.SantaQuestionsViewModel
import com.usuallycoding.opensecretsanta.ui.layout.LocalWideLayout
import com.usuallycoding.opensecretsanta.ui.layout.STANDARD_CONTENT_WIDTH
import com.usuallycoding.opensecretsanta.ui.letters.InboxContent
import com.usuallycoding.opensecretsanta.ui.letters.InboxViewModel
import com.usuallycoding.opensecretsanta.ui.letters.SendLetterContent
import com.usuallycoding.opensecretsanta.ui.letters.SendLetterViewModel
import com.usuallycoding.opensecretsanta.ui.load.PullPage
import com.usuallycoding.opensecretsanta.ui.navigation.PageTab
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
fun ReceiverPage(
    groupId: Long,
    wishlist: WishlistViewModel,
    onBack: () -> Unit,
    onTitle: (String?) -> Unit,
    tab: PageTab = PageTab.Wishlist,
    letters: SendLetterViewModel? = null,
) {
    val wishlistState by wishlist.data.collectAsState()
    val phase = wishlistState.value?.phase
    // the questions exist once the names are drawn: the tab, and the request behind it, only then
    val questions = if (phase == GroupPhase.Active || phase == GroupPhase.Archived) {
        koinViewModel<SantaQuestionsViewModel>(key = "santa-questions-$groupId") { parametersOf(groupId) }
    } else {
        null
    }
    val questionsState = questions?.data?.collectAsState()?.value
    PageTitle(if (questions == null) Strings.TITLE_YOUR_WISHLIST else Strings.TITLE_RECEIVING, onTitle)

    if (wishlistState.isGone() || questionsState?.isGone() == true) {
        GroupGone(onBack)
        return
    }
    if (questions == null) {
        // one section only: on a wide window it keeps the standard width, in the middle
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = STANDARD_CONTENT_WIDTH).fillMaxSize()) {
                if (LocalWideLayout.current) {
                    WideColumns(listOf(Strings.TITLE_YOUR_WISHLIST)) {
                        Box(Modifier.fillMaxSize()) {
                            PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }, bottomSpace = fabClearance()) { WishlistSection(wishlist) }
                            WishlistFab(wishlist)
                        }
                    }
                } else {
                    PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }, bottomSpace = fabClearance()) { WishlistSection(wishlist) }
                    WishlistFab(wishlist)
                }
            }
        }
        return
    }
    // a wide window shows the three sections side by side, each with its own button in the top bar
    if (LocalWideLayout.current && letters != null) {
        WideColumns(listOf(Strings.TAB_YOUR_WISHLIST, Strings.TAB_RESPOND, Strings.TITLE_SEND_LETTER)) { index ->
            when (index) {
                0 -> Box(Modifier.fillMaxSize()) {
                    PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }, bottomSpace = fabClearance()) { WishlistSection(wishlist) }
                    WishlistFab(wishlist)
                }
                1 -> PullPage(loading = questionsState?.loading == true, onRefresh = { questions.refresh() }) { SantaQuestionsSection(questions) }
                else -> SendLetterContent(letters)
            }
        }
        return
    }
    TabbedPages(
        Strings.TAB_YOUR_WISHLIST,
        Strings.TAB_RESPOND,
        tab,
        onShown = { page -> if (page == 0) wishlist.refresh() else questions.refresh() },
    ) { page, _ ->
        if (page == 0) {
            Box(Modifier.fillMaxSize()) {
                PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }, bottomSpace = fabClearance()) { WishlistSection(wishlist) }
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
fun GifterPage(
    wishlist: ReceiverWishlistViewModel,
    questions: ReceiverViewModel,
    onBack: () -> Unit,
    onTitle: (String?) -> Unit,
    tab: PageTab = PageTab.Wishlist,
    inbox: InboxViewModel? = null,
) {
    val wishlistState by wishlist.state.collectAsState()
    val questionsState by questions.data.collectAsState()
    // the receiver's name, once it is known; a failed refresh keeps the name that was loaded before
    val receiver = questionsState.value?.receiver?.username
    PageTitle(Strings.givingTo(receiver), onTitle)

    if (wishlistState.isGone() || questionsState.isGone()) {
        GroupGone(onBack)
        return
    }
    // a wide window shows the three sections side by side, the questions with their button in the top bar
    if (LocalWideLayout.current && inbox != null) {
        WideColumns(listOf(Strings.receiverWishlistTab(receiver), Strings.receiverQuestionsTab(receiver), Strings.lettersFrom(receiver))) { index ->
            when (index) {
                0 -> PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }) { ReceiverWishlistSection(wishlist, true) }
                1 -> Box(Modifier.fillMaxSize()) {
                    PullPage(loading = questionsState.loading, onRefresh = { questions.refresh() }, bottomSpace = fabClearance()) { ReceiverQuestionsSection(questions, true) }
                    AskQuestionFab(questions)
                }
                else -> InboxContent(inbox)
            }
        }
        return
    }
    TabbedPages(
        Strings.receiverWishlistTab(receiver),
        Strings.receiverQuestionsTab(receiver),
        tab,
        onShown = { page -> if (page == 0) wishlist.refresh() else questions.refresh() },
    ) { page, shown ->
        if (page == 0) {
            PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }) { ReceiverWishlistSection(wishlist, shown) }
        } else {
            Box(Modifier.fillMaxSize()) {
                PullPage(loading = questionsState.loading, onRefresh = { questions.refresh() }, bottomSpace = fabClearance()) { ReceiverQuestionsSection(questions, shown) }
                AskQuestionFab(questions)
            }
        }
    }
}

/** Tells the top bar the page's title while the page is shown, and takes it back when the page is left. */
@Composable
internal fun PageTitle(title: String, onTitle: (String?) -> Unit) {
    val latest by rememberUpdatedState(onTitle)
    LaunchedEffect(title) { latest(title) }
    DisposableEffect(Unit) { onDispose { latest(null) } }
}

/**
 * The two tabs of the two pages, labelled [first] and [second]: a tab row and the content of the shown tab, which can be changed
 * by choosing a tab or by swiping the content sideways; the page opens on [opensOn]. A label that is too long for its tab ends in an ellipsis. [onShown]
 * reloads a tab when it is shown (a tab that was left may be stale). A wide window has no tabs: the Giving and Receiving pages show their sections side by side.
 */
@Composable
private fun TabbedPages(first: String, second: String, opensOn: PageTab, onShown: (Int) -> Unit, page: @Composable (index: Int, shown: Boolean) -> Unit) {
    val pager = rememberPagerState(initialPage = opensOn.ordinal) { 2 }
    val scope = rememberCoroutineScope()
    val latestOnShown by rememberUpdatedState(onShown)
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect { latestOnShown(it) }
    }
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = pager.currentPage) {
            Tab(selected = pager.currentPage == 0, onClick = { scope.launch { pager.animateScrollToPage(0) } }, text = { TabLabel(first) })
            Tab(selected = pager.currentPage == 1, onClick = { scope.launch { pager.animateScrollToPage(1) } }, text = { TabLabel(second) })
        }
        HorizontalPager(pager, Modifier.weight(1f), beyondViewportPageCount = 1) { index -> page(index, pager.currentPage == index) }
    }
}

@Composable
private fun TabLabel(label: String) = Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
