package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistFab
import com.usuallycoding.opensecretsanta.ui.gift.AskQuestionFab
import com.usuallycoding.opensecretsanta.ui.form.fabClearance
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.usuallycoding.opensecretsanta.ui.layout.STANDARD_CONTENT_WIDTH
import com.usuallycoding.opensecretsanta.ui.letters.InboxContent
import com.usuallycoding.opensecretsanta.ui.letters.InboxViewModel
import com.usuallycoding.opensecretsanta.ui.letters.SendLetterContent
import com.usuallycoding.opensecretsanta.ui.letters.SendLetterViewModel
import com.usuallycoding.opensecretsanta.ui.load.PullPage
import com.usuallycoding.opensecretsanta.ui.wishlist.ReceiverWishlistSection
import com.usuallycoding.opensecretsanta.ui.wishlist.ReceiverWishlistViewModel
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistSection
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The Receiving page of a wide window: the person's own wishlist, the questions their Santa asked and the Outbox side by side, each under
 * its own heading with its own button. A Pending group has only the wishlist, at the standard width. Nothing on it says who the Santa is.
 */
@Composable
fun ReceiverPage(
    groupId: Long,
    wishlist: WishlistViewModel,
    letters: SendLetterViewModel,
    onBack: () -> Unit,
    onTitle: (String?) -> Unit,
) {
    val wishlistState by wishlist.data.collectAsState()
    val phase = wishlistState.value?.phase
    // the questions exist once the names are drawn: the column, and the request behind it, only then
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
        // one section only: it keeps the standard width, in the middle
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = STANDARD_CONTENT_WIDTH).fillMaxSize()) {
                WideColumns(listOf(Strings.TITLE_YOUR_WISHLIST)) {
                    Box(Modifier.fillMaxSize()) {
                        PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }, bottomSpace = fabClearance()) { WishlistSection(wishlist) }
                        WishlistFab(wishlist)
                    }
                }
            }
        }
        return
    }
    // the three sections side by side, each with its own button at the end of its heading
    WideColumns(listOf(Strings.TAB_YOUR_WISHLIST, Strings.TAB_RESPOND, Strings.TITLE_SEND_LETTER)) { index ->
        when (index) {
            0 -> Box(Modifier.fillMaxSize()) {
                PullPage(loading = wishlistState.loading, onRefresh = { wishlist.refresh() }, bottomSpace = fabClearance()) { WishlistSection(wishlist) }
                WishlistFab(wishlist)
            }
            1 -> SantaQuestionsSection(questions)
            else -> SendLetterContent(letters)
        }
    }
}

/**
 * The Giving page of a wide window: what the person's receiver wants, read-only, the person's own questions to the receiver with the
 * answers, and the Inbox, side by side, each under its own heading.
 */
@Composable
fun GifterPage(
    wishlist: ReceiverWishlistViewModel,
    questions: ReceiverViewModel,
    inbox: InboxViewModel,
    onBack: () -> Unit,
    onTitle: (String?) -> Unit,
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
    WideColumns(listOf(Strings.receiverWishlistTab(receiver), Strings.receiverQuestionsTab(receiver), Strings.lettersFrom(receiver))) { index ->
        when (index) {
            0 -> ReceiverWishlistSection(wishlist, true)
            1 -> Box(Modifier.fillMaxSize()) {
                ReceiverQuestionsSection(questions, true, bottomSpace = fabClearance())
                AskQuestionFab(questions)
            }
            else -> InboxContent(inbox)
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
