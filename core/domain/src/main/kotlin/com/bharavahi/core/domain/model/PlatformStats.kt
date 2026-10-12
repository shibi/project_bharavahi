package com.bharavahi.core.domain.model

data class PlatformStats(
    val activeOrganizations: Int,
    val suspendedOrganizations: Int,
    val totalMembers: Long,
    val totalEvents: Long,
) {
    val totalOrganizations: Int get() = activeOrganizations + suspendedOrganizations
}

data class Dashboard(
    val stats: PlatformStats,
    val recentActivity: List<ActivityLog>,
)
