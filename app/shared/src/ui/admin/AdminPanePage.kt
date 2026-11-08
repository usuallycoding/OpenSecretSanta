package com.usuallycoding.opensecretsanta.ui.admin

import androidx.compose.runtime.Composable
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.announcements.AnnouncementsPage
import com.usuallycoding.opensecretsanta.ui.announcements.AnnouncementsViewModel
import com.usuallycoding.opensecretsanta.ui.groups.PageTitle
import com.usuallycoding.opensecretsanta.ui.navigation.AdminPane
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** An owner's pane as a page of its own, on a narrow window: the rules, one pair of the draw or the announcements. */
@Composable
fun AdminPanePage(pane: AdminPane, groupId: Long, me: String, draw: DrawState, onBack: () -> Unit, onTitle: (String?) -> Unit) {
    when (pane) {
        AdminPane.Rules -> DrawRulesScreen(rulesViewModel(groupId, me))
        is AdminPane.Pair -> {
            PageTitle(Strings.adminPair(pane.santa, pane.receiver), onTitle)
            PairPage(groupId, pane.santa, pane.receiver, draw, onBack)
        }
        AdminPane.Announcements -> AnnouncementsPage(
            koinViewModel<AnnouncementsViewModel>(key = "announcements-$groupId-owner") { parametersOf(groupId, true) },
            onBack = onBack,
        )
    }
}
