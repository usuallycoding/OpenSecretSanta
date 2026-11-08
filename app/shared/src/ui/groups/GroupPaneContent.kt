package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.announcements.AnnouncementsPage
import com.usuallycoding.opensecretsanta.ui.announcements.AnnouncementsViewModel
import com.usuallycoding.opensecretsanta.ui.form.fabClearance
import com.usuallycoding.opensecretsanta.ui.gift.AskQuestionFab
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverQuestionsSection
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverViewModel
import com.usuallycoding.opensecretsanta.ui.gift.SantaQuestionsSection
import com.usuallycoding.opensecretsanta.ui.gift.SantaQuestionsViewModel
import com.usuallycoding.opensecretsanta.ui.letters.InboxContent
import com.usuallycoding.opensecretsanta.ui.letters.InboxViewModel
import com.usuallycoding.opensecretsanta.ui.letters.SendLetterContent
import com.usuallycoding.opensecretsanta.ui.letters.SendLetterViewModel
import com.usuallycoding.opensecretsanta.ui.load.LoadContent
import com.usuallycoding.opensecretsanta.ui.load.PullPage
import com.usuallycoding.opensecretsanta.ui.navigation.GroupPane
import com.usuallycoding.opensecretsanta.ui.wishlist.ReceiverWishlistSection
import com.usuallycoding.opensecretsanta.ui.wishlist.ReceiverWishlistViewModel
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistFab
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistSection
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The title a pane has, for a receiver whose username is [receiver], or null while it is not known. */
internal fun paneTitle(pane: GroupPane, receiver: String?): String = when (pane) {
    GroupPane.Announcements -> Strings.ANNOUNCEMENTS
    GroupPane.GifterWishlist -> Strings.receiverWishlistTab(receiver)
    GroupPane.GifterQuestions -> Strings.receiverQuestionsTab(receiver)
    GroupPane.Inbox -> Strings.lettersFrom(receiver)
    GroupPane.ReceiverWishlist -> Strings.TAB_YOUR_WISHLIST
    GroupPane.ReceiverQuestions -> Strings.TAB_RESPOND
    GroupPane.Outbox -> Strings.TITLE_SEND_LETTER
}

/**
 * [paneTitle] with the receiver's username read from the view model the pane uses anyway, which is shared by everything that shows the
 * same group's pages: the Inbox has it with its letters and the receiver's other pages with the receiver.
 */
@Composable
internal fun paneTitleOf(pane: GroupPane, groupId: Long): String {
    val receiver = when (pane) {
        GroupPane.Inbox -> {
            val inbox = koinViewModel<InboxViewModel>(key = "inbox-$groupId") { parametersOf(groupId) }
            inbox.data.collectAsState().value.value?.receiver
        }
        GroupPane.GifterWishlist, GroupPane.GifterQuestions -> {
            val questions = koinViewModel<ReceiverViewModel>(key = "receiver-$groupId") { parametersOf(groupId) }
            questions.data.collectAsState().value.value?.receiver?.username
        }
        else -> null
    }
    return paneTitle(pane, receiver)
}

/**
 * What a pane shows, the same on a page of its own and in the pane beside the group page: the existing sections, each with its own view
 * model, which is shared with every other place that shows the same thing. A group that is gone says so.
 */
@Composable
internal fun GroupPaneContent(pane: GroupPane, groupId: Long, onBack: () -> Unit) {
    when (pane) {
        GroupPane.Announcements ->
            AnnouncementsPage(koinViewModel<AnnouncementsViewModel>(key = "announcements-$groupId-member") { parametersOf(groupId, false) }, onBack)
        GroupPane.GifterWishlist -> {
            val wishlist = koinViewModel<ReceiverWishlistViewModel>(key = "receiver-wishlist-$groupId") { parametersOf(groupId) }
            val state by wishlist.state.collectAsState()
            if (state.isGone()) GroupGone(onBack) else ReceiverWishlistSection(wishlist, true)
        }
        GroupPane.GifterQuestions -> {
            val questions = koinViewModel<ReceiverViewModel>(key = "receiver-$groupId") { parametersOf(groupId) }
            val state by questions.data.collectAsState()
            if (state.isGone()) {
                GroupGone(onBack)
            } else {
                Box(Modifier.fillMaxSize()) {
                    ReceiverQuestionsSection(questions, true, bottomSpace = fabClearance())
                    AskQuestionFab(questions)
                }
            }
        }
        GroupPane.Inbox -> {
            val inbox = koinViewModel<InboxViewModel>(key = "inbox-$groupId") { parametersOf(groupId) }
            val state by inbox.data.collectAsState()
            if (state.isGone()) GroupGone(onBack) else InboxContent(inbox)
        }
        GroupPane.ReceiverWishlist -> {
            val wishlist = koinViewModel<WishlistViewModel>(key = "wishlist-$groupId") { parametersOf(groupId) }
            val state by wishlist.data.collectAsState()
            if (state.isGone()) {
                GroupGone(onBack)
            } else {
                Box(Modifier.fillMaxSize()) {
                    PullPage(loading = state.loading, onRefresh = { wishlist.refresh() }, bottomSpace = fabClearance()) { WishlistSection(wishlist) }
                    WishlistFab(wishlist)
                }
            }
        }
        GroupPane.ReceiverQuestions -> {
            val questions = koinViewModel<SantaQuestionsViewModel>(key = "santa-questions-$groupId") { parametersOf(groupId) }
            val state by questions.data.collectAsState()
            if (state.isGone()) GroupGone(onBack) else SantaQuestionsSection(questions)
        }
        GroupPane.Outbox -> {
            val letters = koinViewModel<SendLetterViewModel>(key = "send-letter-$groupId") { parametersOf(groupId) }
            val state by letters.data.collectAsState()
            if (state.isGone()) GroupGone(onBack) else SendLetterContent(letters)
        }
    }
}

/**
 * Shows [content] once the group's phase is known, with progress until then and the failure with "Try again" when it cannot be read,
 * and says so when the group is gone. The group's own view model is the one its page uses.
 */
@Composable
internal fun WithGroupPhase(groupId: Long, me: String, onBack: () -> Unit, content: @Composable (GroupPhase) -> Unit) {
    val group = koinViewModel<GroupDetailViewModel>(key = "group-$groupId") { parametersOf(groupId, me) }
    val state by group.state.collectAsState()
    val phase = state.value?.phase
    when {
        state.isGone() -> GroupGone(onBack)
        phase == null -> LoadContent(state, onRetry = { group.refresh() }) { }
        else -> content(phase)
    }
}

/**
 * A pane as a page of its own on a small screen, titled and with the group's phase deciding what it is: a pane that a Pending group does
 * not have is replaced, through [onReplace], by the pane that stands in for it, or by the group's page when there is none.
 */
@Composable
internal fun GroupPaneRoute(pane: GroupPane, groupId: Long, me: String, onBack: () -> Unit, onTitle: (String?) -> Unit, onReplace: (GroupPane?) -> Unit) {
    WithGroupPhase(groupId, me, onBack) { phase ->
        val allowed = pane.within(phase)
        if (allowed == pane) {
            PageTitle(paneTitleOf(pane, groupId), onTitle)
            GroupPaneContent(pane, groupId, onBack)
        } else {
            LaunchedEffect(phase) { onReplace(allowed) }
        }
    }
}
