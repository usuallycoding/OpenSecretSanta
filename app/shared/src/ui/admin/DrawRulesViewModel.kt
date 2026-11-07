package com.usuallycoding.opensecretsanta.ui.admin

import androidx.lifecycle.ViewModel
import com.usuallycoding.opensecretsanta.contract.AddDrawRuleRequest
import com.usuallycoding.opensecretsanta.data.api.ApiError
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.map
import com.usuallycoding.opensecretsanta.domain.group.RuleKind
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.load.LoadState
import com.usuallycoding.opensecretsanta.ui.load.Loader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The owner's draw rules of one group. They are changed while the group is Pending and read after that. The server
 * decides whether a rule can be added, and says why not; what it answers is what is shown.
 */
class DrawRulesViewModel(
    val groupId: Long,
    val me: String,
    private val api: ApiClient,
    private val scope: CoroutineScope,
) : ViewModel(scope) {
    private val _ui = MutableStateFlow(DrawRulesUi())
    private val loader = Loader(scope) {
        when (val group = api.adminGroup(groupId)) {
            is ApiResult.Failure -> group
            is ApiResult.Success -> {
                val loaded = group.value.toGroup()
                api.adminRules(groupId).map { answer ->
                    DrawRulesData(
                        phase = loaded.phase,
                        participants = loaded.participants.map { it.username },
                        rules = answer.rules.mapNotNull { it.toItem() }.inReadingOrder(),
                        fewDraws = answer.fewDraws,
                    )
                }
            }
        }
    }

    val data: StateFlow<LoadState<DrawRulesData>> = loader.state
    val ui: StateFlow<DrawRulesUi> = _ui.asStateFlow()

    /** The owner takes part in the group, so the rules show them part of the draw. */
    val ownerTakesPart: Boolean get() = loader.state.value.value?.participants?.contains(me) == true

    fun refresh(): Job = loader.refresh()

    fun setGiver(name: String?) = changeForm { it.copy(giver = name) }

    fun setReceiver(name: String?) = changeForm { it.copy(receiver = name) }

    fun setKind(kind: RuleKind) = changeForm { it.copy(kind = kind) }

    fun setBothWays(on: Boolean) = changeForm { it.copy(bothWays = on) }

    private fun changeForm(change: (RuleForm) -> RuleForm) = _ui.update { it.copy(form = change(it.form), message = null) }

    /** Sends the form; returns the running work, or null when nothing is sent. The form is kept when the rule is refused. */
    fun add(): Job? {
        val form = _ui.value.form
        if (_ui.value.busy || !form.canAdd || loader.state.value.value?.canEdit != true) return null
        _ui.update { it.copy(busy = true, message = null) }
        return scope.launch {
            when (val result = api.addAdminRule(groupId, AddDrawRuleRequest(form.giver!!, form.receiver!!, form.kind.storedName, form.bothWays))) {
                is ApiResult.Success -> {
                    loader.refresh().join()
                    _ui.update { it.copy(busy = false, form = RuleForm()) }
                }
                is ApiResult.Failure -> failed(result.error)
            }
        }
    }

    fun delete(rule: DrawRuleItem): Job? {
        if (_ui.value.busy || loader.state.value.value?.canEdit != true) return null
        _ui.update { it.copy(busy = true, message = null) }
        return scope.launch {
            when (val result = api.deleteAdminRule(groupId, rule.id)) {
                is ApiResult.Success -> {
                    loader.refresh().join()
                    _ui.update { it.copy(busy = false) }
                }
                is ApiResult.Failure -> failed(result.error)
            }
        }
    }

    private suspend fun failed(error: ApiError) {
        val message = when {
            error is ApiError.Rejected && error.code == "rules_conflict" -> error.message
            error is ApiError.Validation -> error.message
            error == ApiError.Network -> Strings.SERVER_UNREACHABLE
            else -> null
        }
        if (message != null) {
            _ui.update { it.copy(busy = false, message = message) }
        } else {
            // the group changed behind the screen (no longer Pending, a rule already gone): read it again
            loader.refresh().join()
            _ui.update { it.copy(busy = false, message = Strings.SERVER_FAILED.takeIf { error !is ApiError.Rejected }) }
        }
    }
}
