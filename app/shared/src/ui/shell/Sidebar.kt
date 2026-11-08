package com.usuallycoding.opensecretsanta.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.gift.SantaQuestionsViewModel
import com.usuallycoding.opensecretsanta.ui.gift.countBadgeText
import com.usuallycoding.opensecretsanta.ui.gift.unansweredCount
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.GroupRow
import com.usuallycoding.opensecretsanta.ui.groups.GroupsViewModel
import com.usuallycoding.opensecretsanta.ui.groups.groupPhase
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupsViewModel
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupEditRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupNewRoute
import com.usuallycoding.opensecretsanta.ui.navigation.adminGroupIdOfPane
import com.usuallycoding.opensecretsanta.ui.navigation.adminPaneRoute
import com.usuallycoding.opensecretsanta.ui.navigation.isAdminPane
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AnnouncementsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminUserRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminUsersRoute
import com.usuallycoding.opensecretsanta.ui.navigation.DeliveryInfoRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GifterRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.InboxRoute
import com.usuallycoding.opensecretsanta.ui.navigation.ReceiverRoute
import com.usuallycoding.opensecretsanta.ui.navigation.SendLetterRoute
import com.usuallycoding.opensecretsanta.ui.navigation.SettingsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.groupIdOfPane
import com.usuallycoding.opensecretsanta.ui.navigation.paneRoute
import com.usuallycoding.opensecretsanta.ui.readmarks.ReadMarksViewModel
import com.usuallycoding.opensecretsanta.ui.readmarks.known
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** How wide the sidebar of a wide window is. */
private val SIDEBAR_WIDTH = 240.dp

/** The group a screen of the participant view belongs to, or null for the lists and the Settings page. */
internal fun NavBackStackEntry.openGroupId(): Long? = paneRoute()?.let(::groupIdOfPane) ?: when {
    destination.hasRoute<GroupRoute>() -> toRoute<GroupRoute>().id
    destination.hasRoute<GifterRoute>() -> toRoute<GifterRoute>().groupId
    destination.hasRoute<ReceiverRoute>() -> toRoute<ReceiverRoute>().groupId
    destination.hasRoute<DeliveryInfoRoute>() -> toRoute<DeliveryInfoRoute>().groupId
    else -> null
}

/** The route a screen was opened with, as the sidebar's items name them, or null for a screen it has no item for. */
internal fun NavBackStackEntry.sidebarRoute(): Any? = paneRoute()?.let { GroupRoute(groupIdOfPane(it)!!) } ?: when {
    // a pane is read from the group's overview, which stays the selected item
    destination.hasRoute<GroupRoute>() -> toRoute<GroupRoute>()
    destination.hasRoute<GifterRoute>() -> toRoute<GifterRoute>()
    destination.hasRoute<ReceiverRoute>() -> toRoute<ReceiverRoute>()
    destination.hasRoute<SettingsRoute>() -> SettingsRoute
    destination.hasRoute<GroupsRoute>() -> GroupsRoute
    destination.hasRoute<AdminGroupsRoute>() -> AdminGroupsRoute
    destination.hasRoute<AdminUsersRoute>() -> AdminUsersRoute
    else -> null
}

/**
 * The participant view's sidebar: Groups, with the groups listed under it and the open one expanded, and Settings. Choosing an item
 * navigates to the screen it names, replacing the screens of the group on the way instead of stacking them.
 */
@Composable
internal fun ParticipantSidebar(navController: NavController, entry: NavBackStackEntry?, groups: GroupsViewModel) {
    val state by groups.state.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { groups.refresh() }
    // what the person just did on a page may have changed what is new in a group, as it may for the open group's own counts
    LaunchedEffect(entry?.id) { groups.refresh() }
    val current = entry?.sidebarRoute()
    val openId = entry?.openGroupId()
    Sidebar(
        top = {
            SidebarRow(Strings.TITLE_GROUPS, current == GroupsRoute, icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }) {
                // from Settings the tab keeps its place; from a group the item leads back to the list
                if (current == SettingsRoute) navController.openTab(GroupsRoute) else navController.popBackStack<GroupsRoute>(inclusive = false)
            }
        },
        list = {
            for (group in state.value.orEmpty()) {
                if (group.id == openId) {
                    OpenGroup(navController, group, current)
                } else {
                    val spoken = group.unreadAnnouncements.takeIf { it > 0 }?.let { "${group.name}, ${Strings.newAnnouncements(it)}" }
                    SidebarRow(group.name, selected = false, indent = 1, count = group.unreadAnnouncements, spoken = spoken) { navController.openGroup(group.id) }
                }
            }
        },
        bottom = {
            SidebarRow(Strings.TITLE_SETTINGS, current == SettingsRoute, icon = { Icon(Icons.Filled.Settings, contentDescription = null) }) {
                navController.openTab(SettingsRoute)
            }
        },
    )
}

/** The group that is open: its name and what it holds, with what is waiting behind each item badged as the group page does. */
@Composable
private fun OpenGroup(navController: NavController, group: GroupRow, current: Any?) {
    val phase = groupPhase(group.stateKey)
    val drawn = phase == GroupPhase.Active || phase == GroupPhase.Archived
    // the new things and the unanswered questions exist only once the names are drawn
    val marks = if (drawn) koinViewModel<ReadMarksViewModel>(key = "sidebar-read-marks-${group.id}") { parametersOf(group.id) } else null
    val questions = if (phase == GroupPhase.Active) koinViewModel<SantaQuestionsViewModel>(key = "sidebar-questions-${group.id}") { parametersOf(group.id) } else null
    // what the person just did on a page may have changed what is new or unanswered
    LaunchedEffect(current) { marks?.refresh(); questions?.refresh() }
    val unread = marks?.counts?.collectAsState()?.value.known()
    val unanswered = questions?.data?.collectAsState()?.value?.takeIf { it.error == null }?.value?.let { unansweredCount(it.questions, it.canAnswer) } ?: 0

    SidebarRow(group.name, selected = false, indent = 1, bold = true) { navController.openGroupItem(group.id, GroupRoute(group.id)) }
    for (item in groupItems(group.id, phase, unread, unanswered, group.unreadAnnouncements)) {
        SidebarRow(item.label, item.holds(current), indent = 2, count = item.count, spoken = item.spoken) {
            navController.openGroupItem(group.id, item.route)
        }
    }
}

/** The id of the admin group a screen is about, or null for the lists, the users and the Settings page. */
private fun NavBackStackEntry.adminGroupId(): Long? = when {
    destination.hasRoute<AdminGroupRoute>() -> toRoute<AdminGroupRoute>().id
    destination.hasRoute<AdminGroupEditRoute>() -> toRoute<AdminGroupEditRoute>().id
    destination.isAdminPane() -> adminPaneRoute()?.let(::adminGroupIdOfPane)
    else -> null
}

/**
 * The admin view's sidebar: Groups, with the administrator's groups listed under it, Users and the settings both views share. Choosing a
 * group opens it, in place of the group that is shown.
 */
@Composable
internal fun AdminSidebar(navController: NavController, entry: NavBackStackEntry?, groups: AdminGroupsViewModel?) {
    val destination = entry?.destination
    val onUsers = destination?.hasRoute<AdminUsersRoute>() == true || destination?.hasRoute<AdminUserRoute>() == true
    val onSettings = destination?.hasRoute<SettingsRoute>() == true
    val onList = destination?.hasRoute<AdminGroupsRoute>() == true || destination?.hasRoute<AdminGroupNewRoute>() == true
    val openId = entry?.adminGroupId()
    val state = groups?.state?.collectAsState()?.value
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { groups?.refresh() }
    Sidebar(
        top = {
            SidebarRow(Strings.ADMIN_TAB_GROUPS, onList, icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }) {
                // from the other tabs the Groups tab keeps its place; from a group the item leads back to the list
                if (onUsers || onSettings) navController.openTab(AdminGroupsRoute, AdminGroupsRoute) else navController.popBackStack<AdminGroupsRoute>(inclusive = false)
            }
        },
        list = {
            for (group in state?.value.orEmpty()) {
                SidebarRow(group.name, group.id == openId, indent = 1) {
                    navController.navigate(AdminGroupRoute(group.id)) {
                        popUpTo<AdminGroupsRoute>()
                        launchSingleTop = true
                    }
                }
            }
        },
        bottom = {
            SidebarRow(Strings.ADMIN_TAB_USERS, onUsers, icon = { Icon(Icons.Filled.Person, contentDescription = null) }) {
                navController.openTab(AdminUsersRoute, AdminGroupsRoute)
            }
            SidebarRow(Strings.TITLE_SETTINGS, onSettings, icon = { Icon(Icons.Filled.Settings, contentDescription = null) }) {
                navController.openTab(SettingsRoute, AdminGroupsRoute)
            }
        },
    )
}

@Composable
private fun Sidebar(top: @Composable () -> Unit, list: @Composable () -> Unit, bottom: @Composable () -> Unit) {
    PermanentDrawerSheet(Modifier.width(SIDEBAR_WIDTH).fillMaxHeight()) {
        Column(Modifier.fillMaxHeight().padding(vertical = 12.dp)) {
            top()
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { list() }
            bottom()
        }
    }
}

private val INDENT = 12.dp

@Composable
private fun SidebarRow(
    label: String,
    selected: Boolean,
    indent: Int = 0,
    bold: Boolean = false,
    count: Int = 0,
    spoken: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val badge = countBadgeText(count)
    NavigationDrawerItem(
        label = {
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
            )
        },
        selected = selected,
        onClick = onClick,
        icon = icon,
        badge = badge?.let { { Text(it) } },
        modifier = Modifier
            .padding(start = 12.dp + INDENT * indent, end = 12.dp)
            .semantics { if (count > 0 && spoken != null) contentDescription = spoken },
    )
}

/** Opens another group at its Overview, with none of the previous group's screens behind it. */
private fun NavController.openGroup(id: Long) = navigate(GroupRoute(id)) {
    popUpTo<GroupsRoute>()
    launchSingleTop = true
}

/** Opens one of an open group's screens in place of the one shown, so that back leaves the group's pages as it did for a single page. */
private fun NavController.openGroupItem(groupId: Long, route: Any) {
    navigate(route) {
        popUpTo<GroupRoute>()
        launchSingleTop = true
    }
}
