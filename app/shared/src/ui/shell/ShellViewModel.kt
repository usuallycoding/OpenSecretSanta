package com.usuallycoding.opensecretsanta.ui.shell

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.data.session.Account
import com.usuallycoding.opensecretsanta.data.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppView { Participant, Admin }

/**
 * Which of the two views is showing. An administrator starts in the admin view and can switch both
 * ways; anyone else only ever has the participant view. The choice lasts for this run only.
 */
class ShellViewModel(
    val account: Account,
    private val session: SessionManager,
    scope: CoroutineScope,
) : ViewModel(scope) {
    private val _view = MutableStateFlow(if (account.isAdmin) AppView.Admin else AppView.Participant)
    val view: StateFlow<AppView> = _view.asStateFlow()

    val canSwitch: Boolean get() = account.isAdmin

    fun select(view: AppView) {
        if (view == AppView.Admin && !account.isAdmin) return
        _view.value = view
    }

    fun signOut() = session.signOut()
}
