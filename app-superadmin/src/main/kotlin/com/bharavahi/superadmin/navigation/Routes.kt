package com.bharavahi.superadmin.navigation

import kotlinx.serialization.Serializable

@Serializable data object LoginRoute
@Serializable data object DashboardRoute
@Serializable data object CommunitiesRoute
@Serializable data object AddCommunityRoute
@Serializable data class CommunityDetailRoute(val orgId: String)
@Serializable data class EditCommunityRoute(val orgId: String)
@Serializable data object SettingsRoute
