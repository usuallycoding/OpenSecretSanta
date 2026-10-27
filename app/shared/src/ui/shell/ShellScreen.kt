package com.usuallycoding.opensecretsanta.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.GroupDetailScreen
import com.usuallycoding.opensecretsanta.ui.groups.GroupDetailViewModel
import com.usuallycoding.opensecretsanta.ui.groups.GroupsScreen
import com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupsRoute
import com.usuallycoding.opensecretsanta.ui.navigation.ProfileRoute
import com.usuallycoding.opensecretsanta.ui.profile.ProfileScreen

/**
 * The signed-in app under one top bar: the participant view (Groups and Profile tabs, and a group
 * opened from the list) for everyone, and the admin view for administrators, who switch between the
 * two from the top-bar menu. The bar shows where the person is and how to go back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(viewModel: ShellViewModel, onNavigationReady: suspend (NavController) -> Unit = {}) {
    val view by viewModel.view.collectAsState()
    val navController = rememberNavController()
    LaunchedEffect(navController) { onNavigationReady(navController) }
    val destination = navController.currentBackStackEntryAsState().value?.destination
    val participant = view == AppView.Participant
    val onGroup = participant && destination?.hasRoute<GroupRoute>() == true

    Scaffold(
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
                actions = { ShellMenu(viewModel, view) },
            )
        },
        // The tabs stay while a group is open, so switching tabs and coming back finds the group as it was left.
        bottomBar = { if (participant) TabBar(navController, destination) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (participant) {
                NavHost(navController, startDestination = GroupsRoute) {
                    composable<GroupsRoute> {
                        GroupsScreen(koinViewModel(), onOpen = { navController.navigate(GroupRoute(it)) })
                    }
                    composable<GroupRoute> { entry ->
                        val groupId = entry.toRoute<GroupRoute>().id
                        GroupDetailScreen(
                            koinViewModel<GroupDetailViewModel>(key = "group-$groupId") { parametersOf(groupId, viewModel.account.username) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<ProfileRoute> { ProfileScreen(koinViewModel()) }
                }
            } else {
                Centered {
                    Text(Strings.currentView(Strings.ADMIN_VIEW), style = MaterialTheme.typography.titleMedium)
                    Text(Strings.ADMIN_PLACEHOLDER)
                }
            }
        }
    }
}

private fun titleOf(view: AppView, destination: NavDestination?): String = when {
    view == AppView.Admin -> Strings.TITLE_ADMIN
    destination?.hasRoute<GroupRoute>() == true -> Strings.TITLE_GROUP
    destination?.hasRoute<ProfileRoute>() == true -> Strings.TITLE_PROFILE
    else -> Strings.TITLE_GROUPS
}

/** The menu at the right of the bar: the view switch (administrators only) and sign out. */
@Composable
private fun ShellMenu(viewModel: ShellViewModel, view: AppView) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = Strings.MENU)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (viewModel.canSwitch) {
                DropdownMenuItem(
                    text = { Text(if (view == AppView.Admin) Strings.SWITCH_TO_PARTICIPANT else Strings.SWITCH_TO_ADMIN) },
                    onClick = {
                        open = false
                        viewModel.select(if (view == AppView.Admin) AppView.Participant else AppView.Admin)
                    },
                )
            }
            DropdownMenuItem(text = { Text(Strings.SIGN_OUT) }, onClick = { open = false; viewModel.signOut() })
        }
    }
}

/** Groups and Profile. Each tab keeps its own place: switching away and back restores it. */
@Composable
private fun TabBar(navController: NavController, destination: NavDestination?) {
    val onGroups = destination?.hierarchy?.any { it.hasRoute<GroupsRoute>() || it.hasRoute<GroupRoute>() } == true
    val onProfile = destination?.hierarchy?.any { it.hasRoute<ProfileRoute>() } == true
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
    }
}

private fun NavController.openTab(route: Any) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}
