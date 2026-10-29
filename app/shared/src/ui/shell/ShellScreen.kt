package com.usuallycoding.opensecretsanta.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.ui.ExperimentalComposeUiApi
import com.usuallycoding.opensecretsanta.data.push.PushRegistration
import com.usuallycoding.opensecretsanta.ui.form.SwapIcon
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupFormScreen
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupFormViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupScreen
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupsScreen
import com.usuallycoding.opensecretsanta.ui.admin.AdminUserScreen
import com.usuallycoding.opensecretsanta.ui.admin.AdminUserViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminUsersScreen
import com.usuallycoding.opensecretsanta.ui.gift.ReceiverViewModel
import com.usuallycoding.opensecretsanta.ui.groups.GifterPage
import com.usuallycoding.opensecretsanta.ui.groups.GroupPage
import com.usuallycoding.opensecretsanta.ui.groups.ReceiverPage
import com.usuallycoding.opensecretsanta.ui.groups.GroupDetailViewModel
import com.usuallycoding.opensecretsanta.ui.groups.GroupsScreen
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupEditRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupNewRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminUserRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminUsersRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GifterRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.ProfileRoute
import com.usuallycoding.opensecretsanta.ui.navigation.ReceiverRoute
import com.usuallycoding.opensecretsanta.ui.navigation.SettingsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.isAdminScreen
import com.usuallycoding.opensecretsanta.ui.profile.ProfileScreen
import com.usuallycoding.opensecretsanta.ui.settings.SettingsScreen
import com.usuallycoding.opensecretsanta.ui.wishlist.ReceiverWishlistViewModel
import com.usuallycoding.opensecretsanta.ui.wishlist.WishlistViewModel

/**
 * The signed-in app under one top bar: the participant view (Groups and Profile tabs, and a group
 * opened from the list) for everyone, and the admin view for administrators, who switch between the
 * two from the top-bar menu. The bar shows where the person is and how to go back.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ShellScreen(viewModel: ShellViewModel, onNavigationReady: suspend (NavController, isAdmin: Boolean) -> Unit = { _, _ -> }) {
    val view by viewModel.view.collectAsState()
    val navController = rememberNavController()
    LaunchedEffect(navController) { onNavigationReady(navController, viewModel.account.isAdmin) }
    val destination = navController.currentBackStackEntryAsState().value?.destination
    val participant = view == AppView.Participant
    // below a group (its detail or one of its wishlists) there is a way back
    val onGroup = if (participant) destination?.belowGroups() == true else destination?.belowAdminGroups() == true
    // the first view is the one the NavHost starts on; a later switch opens the other view's first screen
    val startRoute = remember { if (viewModel.view.value == AppView.Admin) AdminGroupsRoute else GroupsRoute }
    var shownView by remember { mutableStateOf(view) }
    LaunchedEffect(view) {
        if (view != shownView) {
            shownView = view
            // an address that opened a screen of the other view has already moved there; a screen of neither (Settings) is not of the new view either
            if (navController.currentDestination?.ownView() != view) {
                navController.openView(if (view == AppView.Admin) AdminGroupsRoute else GroupsRoute)
            }
        }
    }
    // a screen of the other view, opened from an address, brings its view along; a screen of neither (Settings) leaves the view as it is
    LaunchedEffect(destination) {
        val own = destination?.ownView()
        if (own != null && own != view) viewModel.select(own)
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val push = koinInject<PushRegistration>()
    // a device that had notifications on tells the server its current token, which may have changed
    LaunchedEffect(Unit) { push.resume() }
    // One standard width for the bars, content and buttons; on a wider window the rest is plain background
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Scaffold(
            modifier = Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(titleOf(view, destination)) },
                    navigationIcon = {
                        if (onGroup) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.BACK)
                            }
                        }
                    },
                    actions = {
                        // the one thing in the top bar: administrators switch between their two views
                        if (viewModel.canSwitch) {
                            IconButton(onClick = { viewModel.select(if (view == AppView.Admin) AppView.Participant else AppView.Admin) }) {
                                Icon(
                                    SwapIcon,
                                    contentDescription = if (view == AppView.Admin) Strings.SWITCH_TO_PARTICIPANT else Strings.SWITCH_TO_ADMIN,
                                )
                            }
                        }
                    },
                )
            },
            // The tabs stay while a group is open, so switching tabs and coming back finds the group as it was left.
            // While the keyboard is open the bar is hidden behind it anyway; leaving it out keeps a form's own keyboard padding exact.
            bottomBar = {
                if (!imeVisible) {
                    if (participant) TabBar(navController, destination) else AdminTabBar(navController, destination)
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                run {
                    NavHost(navController, startDestination = startRoute) {
                        composable<GroupsRoute> {
                            GroupsScreen(koinViewModel(), onOpen = { navController.navigate(GroupRoute(it)) })
                        }
                        composable<GroupRoute> { entry ->
                            val groupId = entry.toRoute<GroupRoute>().id
                            GroupPage(
                                koinViewModel<GroupDetailViewModel>(key = "group-$groupId") { parametersOf(groupId, viewModel.account.username) },
                                me = viewModel.account.username,
                                onBack = { navController.popBackStack() },
                                onOpenGifter = { navController.navigate(GifterRoute(groupId)) },
                                onOpenReceiver = { navController.navigate(ReceiverRoute(groupId)) },
                            )
                        }
                        composable<GifterRoute> { entry ->
                            val groupId = entry.toRoute<GifterRoute>().groupId
                            GifterPage(
                                koinViewModel<ReceiverWishlistViewModel>(key = "receiver-wishlist-$groupId") { parametersOf(groupId) },
                                koinViewModel<ReceiverViewModel>(key = "receiver-$groupId") { parametersOf(groupId) },
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable<ReceiverRoute> { entry ->
                            val groupId = entry.toRoute<ReceiverRoute>().groupId
                            ReceiverPage(
                                groupId,
                                koinViewModel<WishlistViewModel>(key = "wishlist-$groupId") { parametersOf(groupId) },
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable<ProfileRoute> { ProfileScreen(koinViewModel()) }
                        composable<SettingsRoute> { SettingsScreen(onSignOut = viewModel::signOut) }
                        // stand-ins until the admin screens arrive
                        composable<AdminGroupsRoute> {
                            AdminGroupsScreen(
                                koinViewModel(),
                                onOpen = { navController.navigate(AdminGroupRoute(it)) },
                                onNew = { navController.navigate(AdminGroupNewRoute) },
                                onCreated = { navController.navigate(AdminGroupRoute(it)) },
                            )
                        }
                        composable<AdminUsersRoute> {
                            AdminUsersScreen(koinViewModel(), onOpen = { navController.navigate(AdminUserRoute(it)) })
                        }
                        composable<AdminUserRoute> { entry ->
                            val username = entry.toRoute<AdminUserRoute>().username
                            AdminUserScreen(
                                koinViewModel<AdminUserViewModel>(key = "admin-user-$username") { parametersOf(username, viewModel.account.username) },
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable<AdminGroupNewRoute> {
                            AdminGroupFormScreen(
                                koinViewModel<AdminGroupFormViewModel>(key = "admin-group-new") { parametersOf(AdminGroupFormViewModel.NEW_GROUP) },
                                // the new group takes the form's place, so back from it returns to the list
                                onSaved = { id -> navController.navigate(AdminGroupRoute(id)) { popUpTo<AdminGroupNewRoute> { inclusive = true } } },
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable<AdminGroupRoute> { entry ->
                            val groupId = entry.toRoute<AdminGroupRoute>().id
                            AdminGroupScreen(
                                koinViewModel<AdminGroupViewModel>(key = "admin-group-$groupId") { parametersOf(groupId, viewModel.account.username) },
                                onBack = { navController.popBackStack() },
                                onEdit = { navController.navigate(AdminGroupEditRoute(groupId)) },
                                onDeleted = { navController.popBackStack() },
                            )
                        }
                        composable<AdminGroupEditRoute> { entry ->
                            val groupId = entry.toRoute<AdminGroupEditRoute>().id
                            AdminGroupFormScreen(
                                koinViewModel<AdminGroupFormViewModel>(key = "admin-group-edit-$groupId") { parametersOf(groupId) },
                                onSaved = { navController.popBackStack() },
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The widest the signed-in app gets: the bars, the content and the floating buttons share it. */
private val MAX_CONTENT_WIDTH = 640.dp

private fun titleOf(view: AppView, destination: NavDestination?): String = when {
    destination?.hasRoute<SettingsRoute>() == true -> Strings.TITLE_SETTINGS
    destination?.hasRoute<AdminUserRoute>() == true -> Strings.ADMIN_USER
    destination?.hasRoute<AdminUsersRoute>() == true -> Strings.ADMIN_TAB_USERS
    destination?.hasRoute<AdminGroupNewRoute>() == true -> Strings.ADMIN_NEW_GROUP
    destination?.hasRoute<AdminGroupEditRoute>() == true -> Strings.ADMIN_EDIT_GROUP
    destination?.hasRoute<AdminGroupRoute>() == true -> Strings.ADMIN_GROUP
    view == AppView.Admin -> Strings.ADMIN_GROUPS
    destination?.hasRoute<GroupRoute>() == true -> Strings.TITLE_GROUP
    destination?.hasRoute<GifterRoute>() == true -> Strings.TITLE_GIFTER
    destination?.hasRoute<ReceiverRoute>() == true -> Strings.TITLE_RECEIVER
    destination?.hasRoute<ProfileRoute>() == true -> Strings.TITLE_PROFILE
    else -> Strings.TITLE_GROUPS
}

/** Groups, Profile and Settings. Each tab keeps its own place: switching away and back restores it. */
@Composable
private fun TabBar(navController: NavController, destination: NavDestination?) {
    val onGroups = destination?.hierarchy?.any { it.hasRoute<GroupsRoute>() || it.belowGroups() } == true
    val onProfile = destination?.hierarchy?.any { it.hasRoute<ProfileRoute>() } == true
    val onSettings = destination?.hierarchy?.any { it.hasRoute<SettingsRoute>() } == true
    NavigationBar {
        NavigationBarItem(
            selected = onGroups,
            onClick = { navController.openTab(GroupsRoute) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text(Strings.TITLE_GROUPS) },
        )
        NavigationBarItem(
            selected = onProfile,
            onClick = { navController.openTab(ProfileRoute) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text(Strings.TITLE_PROFILE) },
        )
        NavigationBarItem(
            selected = onSettings,
            onClick = { navController.openTab(SettingsRoute) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text(Strings.TITLE_SETTINGS) },
        )
    }
}

/** The view a screen belongs to, or null for a screen both views share and that belongs to neither: the settings page. */
private fun NavDestination.ownView(): AppView? = when {
    hasRoute<SettingsRoute>() -> null
    isAdminScreen() -> AppView.Admin
    else -> AppView.Participant
}

private fun NavDestination.belowGroups(): Boolean =
    hasRoute<GroupRoute>() || hasRoute<GifterRoute>() || hasRoute<ReceiverRoute>()

private fun NavDestination.belowAdminGroups(): Boolean =
    hasRoute<AdminGroupNewRoute>() || hasRoute<AdminGroupRoute>() || hasRoute<AdminGroupEditRoute>() || hasRoute<AdminUserRoute>()

/** The admin view's tabs: the groups and the users the administrator manages, and the settings both views share. */
@Composable
private fun AdminTabBar(navController: NavController, destination: NavDestination?) {
    val onUsers = destination?.hierarchy?.any { it.hasRoute<AdminUsersRoute>() || it.hasRoute<AdminUserRoute>() } == true
    val onSettings = destination?.hierarchy?.any { it.hasRoute<SettingsRoute>() } == true
    NavigationBar {
        NavigationBarItem(
            selected = !onUsers && !onSettings,
            onClick = { navController.openTab(AdminGroupsRoute, AdminGroupsRoute) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text(Strings.ADMIN_TAB_GROUPS) },
        )
        NavigationBarItem(
            selected = onUsers,
            onClick = { navController.openTab(AdminUsersRoute, AdminGroupsRoute) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text(Strings.ADMIN_TAB_USERS) },
        )
        NavigationBarItem(
            selected = onSettings,
            onClick = { navController.openTab(SettingsRoute, AdminGroupsRoute) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text(Strings.TITLE_SETTINGS) },
        )
    }
}

/** Opens a view's first screen with nothing of the other view behind it, so back leaves the app from either. */
private fun NavController.openView(route: Any) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

/** Opens a tab, keeping each tab's own place; [root] is the first screen of the view the tab belongs to. */
private fun NavController.openTab(route: Any, root: Any = GroupsRoute) = navigate(route) {
    popUpTo(root) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}
