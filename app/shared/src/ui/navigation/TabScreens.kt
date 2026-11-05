package com.usuallycoding.opensecretsanta.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph

/**
 * The screens the tab bars open. They are opened with the tab bar's own call (`openTab`) wherever they come from, because that
 * call does more than navigate: popping back to the view's first screen with its state saved files an entry under that first
 * screen, even when nothing is popped, and without it the first tap on the first screen's tab brings back the tab it was left from.
 */
object TabScreens {
    fun isTab(route: Any): Boolean = when (route) {
        GroupsRoute, SettingsRoute, AdminGroupsRoute, AdminUsersRoute -> true
        else -> false
    }

    /** The first screen of the view the app started in, which is what the tab bar pops back to. */
    fun rootOf(controller: NavController): Any {
        val first = controller.currentBackStack.value.firstOrNull { it.destination !is NavGraph }?.destination
        return if (first?.hasRoute<AdminGroupsRoute>() == true) AdminGroupsRoute else GroupsRoute
    }
}
