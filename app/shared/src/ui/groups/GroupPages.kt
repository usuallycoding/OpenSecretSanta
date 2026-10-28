package com.usuallycoding.opensecretsanta.ui.groups

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
 * The page for the relationship in which the person receives: their own wishlist and, once the names are drawn, the
 * questions their Santa asked. Nothing on it says who the Santa is.
 */
@Composable
fun ReceiverPage(groupId: Long, wishlist: WishlistViewModel, onBack: () -> Unit) {
    val wishlistState by wishlist.data.collectAsState()
    val phase = wishlistState.value?.phase
    // the questions exist once the names are drawn: the section, and the request behind it, only then
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
    PullPage(
        loading = wishlistState.loading || questionsState?.loading == true,
        onRefresh = { wishlist.refresh(); questions?.refresh() },
    ) {
        WishlistSection(wishlist)
        if (questions != null) SantaQuestionsSection(questions)
    }
}

/**
 * The page for the relationship in which the person gives: what their receiver wants, read-only, and the person's own
 * questions to the receiver with the answers.
 */
@Composable
fun GifterPage(wishlist: ReceiverWishlistViewModel, questions: ReceiverViewModel, onBack: () -> Unit) {
    val wishlistState by wishlist.state.collectAsState()
    val questionsState by questions.data.collectAsState()

    if (wishlistState.isGone() || questionsState.isGone()) {
        GroupGone(onBack)
        return
    }
    PullPage(
        loading = wishlistState.loading || questionsState.loading,
        onRefresh = { wishlist.refresh(); questions.refresh() },
    ) {
        ReceiverWishlistSection(wishlist)
        ReceiverQuestionsSection(questions)
    }
}
