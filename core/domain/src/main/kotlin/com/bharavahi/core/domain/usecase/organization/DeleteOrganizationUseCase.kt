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

/**
 * SA-7: deletes a community after the owner types its name.
 * This is a soft delete: the data is kept, the community is hidden and its admins are locked out,
 * because payment history must never be lost.
 */
class DeleteOrganizationUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val organizations: OrganizationRepository,
    private val activity: ActivityRepository,
) {
    suspend operator fun invoke(organization: Organization, typedName: String): Result<Unit> = domainResult {
        if (!typedName.trim().equals(organization.name.trim(), ignoreCase = false)) {
            throw DomainException.ConfirmationMismatch()
        }
        val actor = auth.currentUser() ?: throw DomainException.NotAuthorized()
        organizations.setStatus(organization.id, OrganizationStatus.DELETED)
        runCatching { activity.log(organization.id, ActivityAction.ORG_DELETED, actor.uid, organization.name) }
        Unit
    }
}
