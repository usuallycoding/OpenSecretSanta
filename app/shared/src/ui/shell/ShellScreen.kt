package com.usuallycoding.opensecretsanta.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import com.usuallycoding.opensecretsanta.ui.layout.OutsideScroll
import com.usuallycoding.opensecretsanta.ui.layout.LocalOutsideScroll
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
import com.usuallycoding.opensecretsanta.ui.groups.GroupsViewModel
import com.usuallycoding.opensecretsanta.ui.layout.LocalPageActions
import com.usuallycoding.opensecretsanta.ui.layout.LocalWideLayout
import com.usuallycoding.opensecretsanta.ui.layout.PageActionHost
import com.usuallycoding.opensecretsanta.ui.layout.STANDARD_CONTENT_WIDTH
import com.usuallycoding.opensecretsanta.ui.layout.isWide
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupFormScreen
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupFormViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupScreen
import com.usuallycoding.opensecretsanta.ui.admin.AdminPanePage
import androidx.compose.material3.CircularProgressIndicator
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupViewModel
import com.usuallycoding.opensecretsanta.ui.admin.AdminGroupsViewModel
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
import com.usuallycoding.opensecretsanta.ui.announcements.AnnouncementsPage
import com.usuallycoding.opensecretsanta.ui.announcements.AnnouncementsViewModel
import com.usuallycoding.opensecretsanta.ui.navigation.AdminAnnouncementsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AnnouncementsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupPairRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupRulesRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminPane
import com.usuallycoding.opensecretsanta.ui.navigation.adminPaneOf
import com.usuallycoding.opensecretsanta.ui.navigation.closeAdminPane
import com.usuallycoding.opensecretsanta.ui.navigation.isAdminPane
import com.usuallycoding.opensecretsanta.ui.navigation.openAdminPane
import com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminUserRoute
import com.usuallycoding.opensecretsanta.ui.navigation.AdminUsersRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GifterRoute
import com.usuallycoding.opensecretsanta.ui.navigation.InboxRoute
import com.usuallycoding.opensecretsanta.ui.navigation.SendLetterRoute
import com.usuallycoding.opensecretsanta.ui.letters.InboxScreen
import com.usuallycoding.opensecretsanta.ui.letters.InboxViewModel
import com.usuallycoding.opensecretsanta.ui.letters.SendLetterScreen
import com.usuallycoding.opensecretsanta.ui.letters.SendLetterViewModel
import com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.DeliveryInfoRoute
import com.usuallycoding.opensecretsanta.ui.navigation.ReceiverRoute
import com.usuallycoding.opensecretsanta.ui.navigation.SettingsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.isGroupPane
import com.usuallycoding.opensecretsanta.ui.groups.GroupPaneRoute
import com.usuallycoding.opensecretsanta.ui.groups.WithGroupPhase
import com.usuallycoding.opensecretsanta.ui.groups.paneTitle
import com.usuallycoding.opensecretsanta.ui.navigation.GifterQuestionsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GifterWishlistRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupPane
import com.usuallycoding.opensecretsanta.ui.navigation.ReceiverQuestionsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.ReceiverWishlistRoute
import com.usuallycoding.opensecretsanta.ui.navigation.closePane
import com.usuallycoding.opensecretsanta.ui.navigation.givingWithin
import com.usuallycoding.opensecretsanta.ui.navigation.openPane
import com.usuallycoding.opensecretsanta.ui.navigation.paneOf
import com.usuallycoding.opensecretsanta.ui.navigation.paneRoute
import com.usuallycoding.opensecretsanta.ui.navigation.replacePane
import com.usuallycoding.opensecretsanta.ui.navigation.isAdminScreen
import com.usuallycoding.opensecretsanta.ui.delivery.DeliveryInfoScreen
import com.usuallycoding.opensecretsanta.ui.delivery.DeliveryInfoViewModel
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
    // the Gifter and Receiver pages are named after what they load, so they tell the bar their title
    var pageTitle by remember { mutableStateOf<String?>(null) }
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
    val backEntry = navController.currentBackStackEntryAsState().value
    // the participant view's groups: the Groups screen lists them and, on a wide window, so does the sidebar
    val groups = koinViewModel<GroupsViewModel>(key = "groups-${viewModel.account.username}")
    // the administrator's groups, which the Groups screen lists and, on a wide window, so does the sidebar; only an administrator may ask for them
    val adminGroups = if (viewModel.account.isAdmin) koinViewModel<AdminGroupsViewModel>(key = "admin-groups-${viewModel.account.username}") else null
    val pageActions = remember { PageActionHost() }
    // the scrollers of the pages that are shown, for the empty space beside a page to scroll
    val outsideScroll = remember { OutsideScroll() }
    val outsideScrollState = rememberScrollableState { delta -> outsideScroll.scrollBy(delta) }
    // no fill of its own: the app's background is behind it, and the snow, if it is on, is over that background and under this
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = isWide(maxWidth)
        CompositionLocalProvider(
            LocalWideLayout provides wide,
            LocalPageActions provides if (wide) pageActions else null,
            LocalOutsideScroll provides outsideScroll,
        ) {
            Row(Modifier.fillMaxSize()) {
                if (wide) {
                    if (participant) ParticipantSidebar(navController, backEntry, groups) else AdminSidebar(navController, backEntry, adminGroups)
                }
                // One standard width for the bars, content and buttons; on a wider window the rest is plain background. Beside a sidebar the
                // top bar is as wide as the area and only the content keeps the standard width.
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    // behind the Scaffold: beside the capped column between 640 and 840dp, where the Scaffold is not (the one inside it covers
                    // the Scaffold's own area); over a page its own scroller takes the gesture
                    Box(Modifier.fillMaxSize().scrollable(outsideScrollState, Orientation.Vertical, reverseDirection = true))
                    Scaffold(
                        modifier = Modifier.widthIn(max = if (wide) Dp.Unspecified else STANDARD_CONTENT_WIDTH).fillMaxSize(),
                        // open space shows the snow; the bars below keep their own fills and hide it
                        containerColor = Color.Transparent,
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text(titleOf(view, destination, pageTitle, wide, backEntry?.paneRoute()?.let(::paneOf))) },
                                navigationIcon = {
                                    if (onGroup) {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.BACK)
                                        }
                                    }
                                },
                                actions = {
                                    // the page's own buttons, which a wide window shows here in the place of floating buttons
                                    for (action in pageActions.actions) {
                                        TextButton(onClick = action.onClick) {
                                            Icon(action.icon, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(action.description)
                                        }
                                    }
                                    // administrators switch between their two views
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
                        // A wide window has the sidebar instead.
                        bottomBar = {
                            if (!imeVisible && !wide) {
                                if (participant) TabBar(navController, destination) else AdminTabBar(navController, destination)
                            }
                        },
        ) { padding ->
            // On a wide window the content keeps one readable width beside the sidebar; the pages that lay out columns of their own use all of it
            val spread = wide && destination?.let {
                it.hasRoute<AdminGroupRoute>() || it.isAdminPane() || it.hasRoute<GroupRoute>() || it.isGroupPane() || it.hasRoute<GifterRoute>() || it.hasRoute<ReceiverRoute>()
            } == true
            Column(Modifier.fillMaxSize().padding(padding)) {
            val handedOverTo by viewModel.handedOverTo.collectAsState()
            handedOverTo?.takeIf { participant }?.let { name ->
                // said once, on the Groups tab the person lands on after handing the administrator role over
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Strings.handedOverNotice(name), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = viewModel::dismissHandedOver) { Text(Strings.OK) }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                // The Scaffold is a surface, which takes every pointer event over its whole area, so the box behind it never sees one
                // there: this one is inside it, behind the page and over the same area, and scrolls the page that is shown where nothing
                // in front handles a wheel or a drag (the empty space beside a page that keeps a readable width)
                Box(Modifier.fillMaxSize().scrollable(outsideScrollState, Orientation.Vertical, reverseDirection = true))
                Box(Modifier.widthIn(max = if (wide && !spread) STANDARD_CONTENT_WIDTH else Dp.Unspecified).fillMaxSize()) {
                    NavHost(navController, startDestination = startRoute) {
                        composable<GroupsRoute> {
                            GroupsScreen(groups, onOpen = { navController.navigate(GroupRoute(it)) })
                        }
                        composable<GroupRoute> { entry ->
                            GroupEntry(navController, viewModel.account.username, entry.toRoute<GroupRoute>().id, pane = null)
                        }
                        // each pane is a page of its own on a small screen and, beside the group's page, a pane on a wide window
                        composable<AnnouncementsRoute> { entry ->
                            PaneEntry(navController, viewModel.account.username, wide, GroupPane.Announcements, entry.toRoute<AnnouncementsRoute>().groupId) { pageTitle = it }
                        }
                        composable<GifterWishlistRoute> { entry ->
                            PaneEntry(navController, viewModel.account.username, wide, GroupPane.GifterWishlist, entry.toRoute<GifterWishlistRoute>().groupId) { pageTitle = it }
                        }
                        composable<GifterQuestionsRoute> { entry ->
                            PaneEntry(navController, viewModel.account.username, wide, GroupPane.GifterQuestions, entry.toRoute<GifterQuestionsRoute>().groupId) { pageTitle = it }
                        }
                        composable<InboxRoute> { entry ->
                            PaneEntry(navController, viewModel.account.username, wide, GroupPane.Inbox, entry.toRoute<InboxRoute>().groupId) { pageTitle = it }
                        }
                        composable<ReceiverWishlistRoute> { entry ->
                            PaneEntry(navController, viewModel.account.username, wide, GroupPane.ReceiverWishlist, entry.toRoute<ReceiverWishlistRoute>().groupId) { pageTitle = it }
                        }
                        composable<ReceiverQuestionsRoute> { entry ->
                            PaneEntry(navController, viewModel.account.username, wide, GroupPane.ReceiverQuestions, entry.toRoute<ReceiverQuestionsRoute>().groupId) { pageTitle = it }
                        }
                        composable<SendLetterRoute> { entry ->
                            PaneEntry(navController, viewModel.account.username, wide, GroupPane.Outbox, entry.toRoute<SendLetterRoute>().groupId) { pageTitle = it }
                        }
                        // the Giving and Receiving pages: three sections side by side on a wide window, the first of them on a small screen
                        composable<GifterRoute> { entry ->
                            val groupId = entry.toRoute<GifterRoute>().groupId
                            if (wide) {
                                WithGroupPhase(groupId, viewModel.account.username, onBack = { navController.popBackStack() }) { phase ->
                                    if (givingWithin(phase)) {
                                        GifterPage(
                                            koinViewModel<ReceiverWishlistViewModel>(key = "receiver-wishlist-$groupId") { parametersOf(groupId) },
                                            koinViewModel<ReceiverViewModel>(key = "receiver-$groupId") { parametersOf(groupId) },
                                            koinViewModel<InboxViewModel>(key = "inbox-$groupId") { parametersOf(groupId) },
                                            onBack = { navController.popBackStack() },
                                            onTitle = { pageTitle = it },
                                        )
                                    } else {
                                        // before the draw there is no receiver to give to
                                        LaunchedEffect(phase) {
                                            navController.navigate(GroupRoute(groupId)) { popUpTo<GifterRoute> { inclusive = true }; launchSingleTop = true }
                                        }
                                    }
                                }
                            } else {
                                LaunchedEffect(groupId) {
                                    navController.navigate(GifterWishlistRoute(groupId)) { popUpTo<GifterRoute> { inclusive = true }; launchSingleTop = true }
                                }
                            }
                        }
                        composable<ReceiverRoute> { entry ->
                            val groupId = entry.toRoute<ReceiverRoute>().groupId
                            if (wide) {
                                ReceiverPage(
                                    groupId,
                                    koinViewModel<WishlistViewModel>(key = "wishlist-$groupId") { parametersOf(groupId) },
                                    koinViewModel<SendLetterViewModel>(key = "send-letter-$groupId") { parametersOf(groupId) },
                                    onBack = { navController.popBackStack() },
                                    onTitle = { pageTitle = it },
                                )
                            } else {
                                LaunchedEffect(groupId) {
                                    navController.navigate(ReceiverWishlistRoute(groupId)) { popUpTo<ReceiverRoute> { inclusive = true }; launchSingleTop = true }
                                }
                            }
                        }
                        composable<DeliveryInfoRoute> { entry ->
                            val groupId = entry.toRoute<DeliveryInfoRoute>().groupId
                            DeliveryInfoScreen(koinViewModel<DeliveryInfoViewModel>(key = "delivery-info-$groupId") { parametersOf(groupId) })
                        }
                        composable<SettingsRoute> { SettingsScreen(onSignOut = viewModel::signOut) }
                        // stand-ins until the admin screens arrive
                        composable<AdminGroupsRoute> {
                            AdminGroupsScreen(
                                adminGroups ?: koinViewModel(),
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
                            AdminGroupEntry(navController, viewModel.account.username, entry.toRoute<AdminGroupRoute>().id, null)
                        }
                        composable<AdminAnnouncementsRoute> { entry ->
                            AdminPaneEntry(navController, viewModel.account.username, wide, AdminPane.Announcements, entry.toRoute<AdminAnnouncementsRoute>().id) { pageTitle = it }
                        }
                        composable<AdminGroupRulesRoute> { entry ->
                            AdminPaneEntry(navController, viewModel.account.username, wide, AdminPane.Rules, entry.toRoute<AdminGroupRulesRoute>().id) { pageTitle = it }
                        }
                        composable<AdminGroupPairRoute> { entry ->
                            val route = entry.toRoute<AdminGroupPairRoute>()
                            AdminPaneEntry(navController, viewModel.account.username, wide, AdminPane.Pair(route.santa, route.receiver), route.id) { pageTitle = it }
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
            }
        }
    }
}


/** The top bar's title; a wide window shows the announcements beside their group's page, so the page keeps its own title there. */
/**
 * The top bar's title. A wide window shows a pane beside its group's page, so the page keeps its own title there; a small screen shows the
 * [pane]'s page, titled by what it set, or by its title without the receiver's name until then.
 */
private fun titleOf(view: AppView, destination: NavDestination?, pageTitle: String?, wide: Boolean, pane: GroupPane?): String = when {
    destination?.hasRoute<SettingsRoute>() == true -> Strings.TITLE_SETTINGS
    destination?.hasRoute<AdminUserRoute>() == true -> Strings.ADMIN_USER
    destination?.hasRoute<AdminUsersRoute>() == true -> Strings.ADMIN_TAB_USERS
    destination?.hasRoute<AdminGroupNewRoute>() == true -> Strings.ADMIN_NEW_GROUP
    destination?.hasRoute<AdminGroupEditRoute>() == true -> Strings.ADMIN_EDIT_GROUP
    destination?.hasRoute<AdminGroupRulesRoute>() == true -> if (wide) Strings.ADMIN_GROUP else Strings.RULES_TITLE
    destination?.hasRoute<AdminGroupPairRoute>() == true -> if (wide) Strings.ADMIN_GROUP else pageTitle ?: Strings.ADMIN_GROUP
    destination?.hasRoute<AdminAnnouncementsRoute>() == true -> if (wide) Strings.ADMIN_GROUP else Strings.ANNOUNCEMENTS
    destination?.hasRoute<AdminGroupRoute>() == true -> Strings.ADMIN_GROUP
    view == AppView.Admin -> Strings.ADMIN_GROUPS
    destination?.hasRoute<GroupRoute>() == true -> Strings.TITLE_GROUP
    pane != null -> if (wide) Strings.TITLE_GROUP else pageTitle ?: paneTitle(pane, null)
    destination?.hasRoute<GifterRoute>() == true -> pageTitle ?: Strings.givingTo(null)
    destination?.hasRoute<ReceiverRoute>() == true -> pageTitle ?: Strings.TITLE_YOUR_WISHLIST
    destination?.hasRoute<DeliveryInfoRoute>() == true -> Strings.TITLE_DELIVERY_INFO
    else -> Strings.TITLE_GROUPS
}

/** Groups and Settings. Each tab keeps its own place: switching away and back restores it. */
@Composable
private fun TabBar(navController: NavController, destination: NavDestination?) {
    val onGroups = destination?.hierarchy?.any { it.hasRoute<GroupsRoute>() || it.belowGroups() } == true
    val onSettings = destination?.hierarchy?.any { it.hasRoute<SettingsRoute>() } == true
    NavigationBar {
        NavigationBarItem(
            selected = onGroups,
            onClick = { navController.openTab(GroupsRoute) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text(Strings.TITLE_GROUPS) },
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
    hasRoute<GroupRoute>() || hasRoute<GifterRoute>() || hasRoute<ReceiverRoute>() || hasRoute<DeliveryInfoRoute>() || isGroupPane()

private fun NavDestination.belowAdminGroups(): Boolean =
    hasRoute<AdminGroupNewRoute>() || hasRoute<AdminGroupRoute>() || hasRoute<AdminGroupEditRoute>() || isAdminPane() || hasRoute<AdminUserRoute>()

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

/** Opens a tab, keeping each tab's own place; [root] is the first screen of the view the tab belongs to. Also used by the web host for an address that names a tab. */
fun NavController.openTab(route: Any, root: Any = GroupsRoute) = navigate(route) {
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

/**
 * A participant's group page. With a [pane], which is the pane's own address, a wide window shows that page beside it; closing the pane
 * goes back to the page's own address, whether the pane was opened from the page or by an address, and opening another pane replaces it.
 */
@Composable
private fun GroupEntry(navController: NavController, me: String, groupId: Long, pane: GroupPane?) {
    GroupPage(
        koinViewModel<GroupDetailViewModel>(key = "group-$groupId") { parametersOf(groupId, me) },
        me = me,
        onBack = { navController.popBackStack() },
        onEditDeliveryInfo = { navController.navigate(DeliveryInfoRoute(groupId)) },
        onOpenPane = { navController.openPane(pane, groupId, it) },
        pane = pane,
        onClosePane = { pane?.let { navController.closePane(it, groupId) } },
        onReplacePane = { to -> pane?.let { navController.replacePane(it, groupId, to) } },
    )
}

/** A pane's route: the group's page with the pane beside it on a wide window, the pane as a page of its own on a small screen. */
@Composable
private fun PaneEntry(navController: NavController, me: String, wide: Boolean, pane: GroupPane, groupId: Long, onTitle: (String?) -> Unit) {
    if (wide) {
        GroupEntry(navController, me, groupId, pane)
    } else {
        GroupPaneRoute(
            pane, groupId, me,
            onBack = { navController.popBackStack() },
            onTitle = onTitle,
            onReplace = { to -> navController.replacePane(pane, groupId, to) },
        )
    }
}

/** The owner's group screen, with [pane] beside it on a wide window; see [GroupEntry]. */
@Composable
private fun AdminGroupEntry(navController: NavController, me: String, groupId: Long, pane: AdminPane?) {
    AdminGroupScreen(
        koinViewModel<AdminGroupViewModel>(key = "admin-group-$groupId") { parametersOf(groupId, me) },
        onBack = { navController.popBackStack() },
        onEdit = { navController.navigate(AdminGroupEditRoute(groupId)) },
        onOpenPane = { to -> navController.openAdminPane(pane, groupId, to) },
        onClosePane = { pane?.let { navController.closeAdminPane(it, groupId) } },
        onDeleted = { navController.popBackStack() },
        pane = pane,
    )
}

/**
 * An owner's pane route: the group's screen with the pane beside it on a wide window, the pane's page on a narrow one. A group that has
 * nothing to show for the pane, or a pair that is not in its draw, gives the group's screen instead, once that is known.
 */
@Composable
private fun AdminPaneEntry(navController: NavController, me: String, wide: Boolean, pane: AdminPane, groupId: Long, onTitle: (String?) -> Unit) {
    if (wide) {
        AdminGroupEntry(navController, me, groupId, pane)
        return
    }
    val group = koinViewModel<AdminGroupViewModel>(key = "admin-group-$groupId") { parametersOf(groupId, me) }
    val state by group.data.collectAsState()
    val draw by group.draw.collectAsState()
    LaunchedEffect(group) { group.refresh() }
    val allowed = pane.within(state.value?.phase, draw)
    LaunchedEffect(allowed) { if (allowed == false) navController.closeAdminPane(pane, groupId) }
    if (allowed == true) {
        AdminPanePage(pane, groupId, me, draw, onBack = { navController.popBackStack() }, onTitle = onTitle)
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}
