package com.bharavahi.core.domain.usecase.organization

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.domain.repository.ActivityRepository
import com.bharavahi.core.domain.repository.AuthRepository
import com.bharavahi.core.domain.repository.OrganizationRepository
import com.bharavahi.core.domain.usecase.domainResult
import javax.inject.Inject

/** SA-6: suspend or reactivate. Suspension locks out every admin of the community. */
class SetOrganizationStatusUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val organizations: OrganizationRepository,
    private val activity: ActivityRepository,
) {
    suspend operator fun invoke(organization: Organization, suspend: Boolean): Result<Unit> = domainResult {
        val actor = auth.currentUser() ?: throw DomainException.NotAuthorized()
        val status = if (suspend) OrganizationStatus.SUSPENDED else OrganizationStatus.ACTIVE
        organizations.setStatus(organization.id, status)
        val action = if (suspend) ActivityAction.ORG_SUSPENDED else ActivityAction.ORG_REACTIVATED
        runCatching { activity.log(organization.id, action, actor.uid, organization.name) }
        Unit
    }
}
