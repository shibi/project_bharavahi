package com.bharavahi.superadmin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bharavahi.superadmin.ui.communities.CommunitiesScreen
import com.bharavahi.superadmin.ui.communityform.CommunityFormScreen
import com.bharavahi.superadmin.ui.dashboard.DashboardScreen
import com.bharavahi.superadmin.ui.detail.CommunityDetailScreen
import com.bharavahi.superadmin.ui.login.LoginScreen
import com.bharavahi.superadmin.ui.settings.SettingsScreen

/**
 * UI-13: plain stack navigation with a top app bar on each screen, no bottom bar.
 * Signing in or out swaps the whole back stack.
 */
@Composable
fun SuperAdminNavHost(
    signedIn: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(signedIn) {
        val current = navController.currentBackStackEntry?.destination ?: return@LaunchedEffect
        val alreadyThere = if (signedIn) current.hasRoute<DashboardRoute>() else current.hasRoute<LoginRoute>()
        if (alreadyThere) return@LaunchedEffect
        val target: Any = if (signedIn) DashboardRoute else LoginRoute
        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    // Fixed at first composition; later sign-in changes are handled by the effect above.
    val startDestination: Any = remember { if (signedIn) DashboardRoute else LoginRoute }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<LoginRoute> { LoginScreen() }

        composable<DashboardRoute> {
            DashboardScreen(
                onOpenCommunities = { navController.navigate(CommunitiesRoute) },
                onOpenCommunity = { navController.navigate(CommunityDetailRoute(it)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }

        composable<CommunitiesRoute> {
            CommunitiesScreen(
                onBack = navController::navigateUp,
                onAdd = { navController.navigate(AddCommunityRoute) },
                onOpenCommunity = { navController.navigate(CommunityDetailRoute(it)) },
            )
        }

        composable<AddCommunityRoute> {
            CommunityFormScreen(
                onBack = navController::navigateUp,
                onSaved = { orgId ->
                    navController.navigate(CommunityDetailRoute(orgId)) {
                        popUpTo<CommunitiesRoute>()
                    }
                },
            )
        }

        composable<EditCommunityRoute> {
            CommunityFormScreen(
                onBack = navController::navigateUp,
                onSaved = { navController.navigateUp() },
            )
        }

        composable<CommunityDetailRoute> {
            CommunityDetailScreen(
                onBack = navController::navigateUp,
                onEdit = { orgId -> navController.navigate(EditCommunityRoute(orgId)) },
                onDeleted = { navController.navigateUp() },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(onBack = navController::navigateUp)
        }
    }
}
