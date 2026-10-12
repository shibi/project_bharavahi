package com.bharavahi.core.domain.usecase.dashboard

import com.bharavahi.core.domain.model.Dashboard
import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.domain.model.PlatformStats
import com.bharavahi.core.domain.repository.ActivityRepository
import com.bharavahi.core.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** SA-2: platform totals and recent activity. */
class ObserveDashboardUseCase @Inject constructor(
    private val organizations: OrganizationRepository,
    private val activity: ActivityRepository,
) {
    operator fun invoke(): Flow<Dashboard> = combine(
        organizations.observeAll(),
        activity.observeRecent(RECENT_LIMIT),
    ) { orgs, recent -> Dashboard(stats = statsOf(orgs), recentActivity = recent) }

    companion object {
        const val RECENT_LIMIT = 20

        fun statsOf(orgs: List<Organization>): PlatformStats {
            val visible = orgs.filter { it.status != OrganizationStatus.DELETED }
            return PlatformStats(
                activeOrganizations = visible.count { it.status == OrganizationStatus.ACTIVE },
                suspendedOrganizations = visible.count { it.status == OrganizationStatus.SUSPENDED },
                totalMembers = visible.sumOf { it.memberCount },
                totalEvents = visible.sumOf { it.eventCount },
            )
        }
    }
}
