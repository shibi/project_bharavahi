package com.bharavahi.core.domain.usecase.organization

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.OrganizationDetails
import com.bharavahi.core.domain.repository.ActivityRepository
import com.bharavahi.core.domain.repository.AuthRepository
import com.bharavahi.core.domain.repository.OrganizationRepository
import com.bharavahi.core.domain.usecase.domainResult
import javax.inject.Inject

/** SA-5: edit a community's details. */
class UpdateOrganizationUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val organizations: OrganizationRepository,
    private val activity: ActivityRepository,
) {
    suspend operator fun invoke(orgId: String, details: OrganizationDetails): Result<Unit> = domainResult {
        val errors = buildMap<Field, ValidationError> { validateDetails(details) }
        if (errors.isNotEmpty()) throw DomainException.Validation(errors)
        val actor = auth.currentUser() ?: throw DomainException.NotAuthorized()

        organizations.update(orgId, details.trimmed())
        runCatching { activity.log(orgId, ActivityAction.ORG_UPDATED, actor.uid, details.name.trim()) }
        Unit
    }
}
