package com.bharavahi.core.domain.usecase.organization

import com.bharavahi.core.domain.model.ActivityLog
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.repository.ActivityRepository
import com.bharavahi.core.domain.repository.OrgAdminRepository
import com.bharavahi.core.domain.repository.OrganizationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class OrganizationDetail(
    val organization: Organization?,
    val admins: List<OrgAdmin>,
    val activity: List<ActivityLog>,
)

/** SA-5: a community with its admins and activity log. */
class ObserveOrganizationDetailUseCase @Inject constructor(
    private val organizations: OrganizationRepository,
    private val admins: OrgAdminRepository,
    private val activity: ActivityRepository,
) {
    operator fun invoke(orgId: String): Flow<OrganizationDetail> = combine(
        organizations.observe(orgId),
        admins.observeAdmins(orgId),
        activity.observeForOrganization(orgId, ACTIVITY_LIMIT),
    ) { org, adminList, logs -> OrganizationDetail(org, adminList, logs) }

    private companion object {
        const val ACTIVITY_LIMIT = 50
    }
}
