package com.bharavahi.org.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bharavahi.org.R
import com.bharavahi.org.ui.dashboard.DashboardScreen
import com.bharavahi.org.ui.eventdetail.EventDetailScreen
import com.bharavahi.org.ui.eventdues.EventDuesScreen
import com.bharavahi.org.ui.eventform.EventFormScreen
import com.bharavahi.org.ui.eventmembers.AddEventMembersScreen
import com.bharavahi.org.ui.events.EventsScreen
import com.bharavahi.org.ui.expenses.AddExpenseScreen
import com.bharavahi.org.ui.expenses.ExpensesScreen
import com.bharavahi.org.ui.login.LoginScreen
import com.bharavahi.org.ui.memberdues.MemberDuesScreen
import com.bharavahi.org.ui.memberform.MemberFormScreen
import com.bharavahi.org.ui.members.MembersScreen
import com.bharavahi.org.ui.payment.MarkPaymentScreen
import com.bharavahi.org.ui.payments.PaymentsScreen
import com.bharavahi.org.ui.profile.ProfileScreen
import com.bharavahi.org.ui.schedule.ScheduleScreen
import com.bharavahi.org.ui.scheduleform.ScheduleFormScreen

private enum class Tab(val route: Any, @StringRes val label: Int, val icon: ImageVector) {
    DASHBOARD(DashboardRoute, R.string.tab_dashboard, Icons.Default.Home),
    EVENTS(EventsRoute, R.string.tab_events, Icons.Default.Event),
    MEMBERS(MembersRoute, R.string.tab_members, Icons.Default.Groups),
    PROFILE(ProfileRoute, R.string.tab_profile, Icons.Default.Person),
}

private fun NavDestination.tab(): Tab? = when {
    hasRoute<DashboardRoute>() -> Tab.DASHBOARD
    hasRoute<EventsRoute>() -> Tab.EVENTS
    hasRoute<MembersRoute>() -> Tab.MEMBERS
    hasRoute<ProfileRoute>() -> Tab.PROFILE
    else -> null
}

/**
 * UI-10: bottom navigation on the four tab screens, with the raised Mark payment button in the middle.
 * Other screens are pushed on top with their own back arrow. Signing in or out swaps the whole back stack.
 */
@Composable
fun OrgNavHost(
    signedIn: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(signedIn) {
        val current = navController.currentBackStackEntry?.destination ?: return@LaunchedEffect
        // Signed in anywhere but Login (for example after rotation) means the stack is already right.
        val onLogin = current.hasRoute<LoginRoute>()
        val alreadyThere = if (signedIn) !onLogin else onLogin
        if (alreadyThere) return@LaunchedEffect
        val target: Any = if (signedIn) DashboardRoute else LoginRoute
        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    // Fixed at first composition; later sign-in changes are handled by the effect above.
    val startDestination: Any = remember { if (signedIn) DashboardRoute else LoginRoute }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.tab()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                BottomBar(
                    current = currentTab,
                    onSelect = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo<DashboardRoute> { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMarkPayment = { navController.navigate(MarkPaymentRoute()) },
                )
            }
        },
    ) { inner ->
        val padding = PaddingValues(bottom = inner.calculateBottomPadding())
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<LoginRoute> { LoginScreen() }

            composable<DashboardRoute> {
                DashboardScreen(
                    onOpenEvent = { navController.navigate(EventDetailRoute(it)) },
                    onCreateEvent = { navController.navigate(CreateEventRoute) },
                    onAddMember = { navController.navigate(AddMemberRoute) },
                )
            }
            composable<EventsRoute> {
                EventsScreen(
                    onOpenEvent = { navController.navigate(EventDetailRoute(it)) },
                    onCreateEvent = { navController.navigate(CreateEventRoute) },
                )
            }
            composable<MembersRoute> {
                MembersScreen(
                    onAddMember = { navController.navigate(AddMemberRoute) },
                    onOpenMember = { navController.navigate(EditMemberRoute(it)) },
                )
            }
            composable<ProfileRoute> { ProfileScreen() }

            composable<AddMemberRoute> {
                MemberFormScreen(onBack = navController::navigateUp, onSaved = { navController.navigateUp() })
            }
            composable<EditMemberRoute> {
                MemberFormScreen(onBack = navController::navigateUp, onSaved = { navController.navigateUp() })
            }

            composable<CreateEventRoute> {
                EventFormScreen(
                    onBack = navController::navigateUp,
                    onSaved = { eventId ->
                        navController.navigate(EventDetailRoute(eventId)) { popUpTo<CreateEventRoute> { inclusive = true } }
                    },
                )
            }
            composable<EditEventRoute> {
                EventFormScreen(onBack = navController::navigateUp, onSaved = { navController.navigateUp() })
            }
            composable<EventDetailRoute> {
                EventDetailScreen(
                    onBack = navController::navigateUp,
                    onEdit = { navController.navigate(EditEventRoute(it)) },
                    onAddMembers = { navController.navigate(AddEventMembersRoute(it)) },
                    onAddSchedule = { navController.navigate(AddScheduleRoute(it)) },
                    onOpenSchedule = { eventId, scheduleId -> navController.navigate(ScheduleRoute(eventId, scheduleId)) },
                    onMarkPayment = { navController.navigate(MarkPaymentRoute(eventId = it)) },
                    onOpenDues = { navController.navigate(EventDuesRoute(it)) },
                    onOpenPayments = { navController.navigate(PaymentsRoute(it)) },
                    onOpenExpenses = { navController.navigate(ExpensesRoute(it)) },
                )
            }
            composable<AddEventMembersRoute> {
                AddEventMembersScreen(onBack = navController::navigateUp, onSaved = { navController.navigateUp() })
            }

            composable<AddScheduleRoute> {
                ScheduleFormScreen(onBack = navController::navigateUp, onSaved = { navController.navigateUp() })
            }
            composable<EditScheduleRoute> {
                ScheduleFormScreen(onBack = navController::navigateUp, onSaved = { navController.navigateUp() })
            }
            composable<ScheduleRoute> {
                ScheduleScreen(
                    onBack = navController::navigateUp,
                    onEdit = { eventId, scheduleId -> navController.navigate(EditScheduleRoute(eventId, scheduleId)) },
                    onDeleted = { navController.navigateUp() },
                )
            }

            composable<MarkPaymentRoute> { MarkPaymentScreen(onBack = navController::navigateUp) }
            composable<EventDuesRoute> {
                EventDuesScreen(
                    onBack = navController::navigateUp,
                    onOpenMember = { eventId, memberId -> navController.navigate(MemberDuesRoute(eventId, memberId)) },
                )
            }
            composable<MemberDuesRoute> {
                MemberDuesScreen(
                    onBack = navController::navigateUp,
                    onMarkPayment = { eventId, memberId -> navController.navigate(MarkPaymentRoute(eventId, memberId)) },
                )
            }
            composable<PaymentsRoute> { PaymentsScreen(onBack = navController::navigateUp) }
            composable<ExpensesRoute> {
                ExpensesScreen(onBack = navController::navigateUp, onAdd = { navController.navigate(AddExpenseRoute(it)) })
            }
            composable<AddExpenseRoute> {
                AddExpenseScreen(onBack = navController::navigateUp, onSaved = { navController.navigateUp() })
            }
        }
    }
}

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit, onMarkPayment: () -> Unit) {
    NavigationBar {
        Tab.entries.forEachIndexed { index, tab ->
            if (index == 2) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    FloatingActionButton(
                        onClick = onMarkPayment,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.mark_payment))
                    }
                }
            }
            NavigationBarItem(
                selected = tab == current,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}
