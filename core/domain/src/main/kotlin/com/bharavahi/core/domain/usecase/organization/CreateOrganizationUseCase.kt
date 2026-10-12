package com.bharavahi.core.domain.usecase.organization

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.NewAdmin
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.model.OrganizationDetails
import com.bharavahi.core.domain.repository.ActivityRepository
import com.bharavahi.core.domain.repository.AdminAccountGateway
import com.bharavahi.core.domain.repository.AuthRepository
import com.bharavahi.core.domain.repository.OrganizationRepository
import com.bharavahi.core.domain.usecase.domainResult
import javax.inject.Inject

/**
 * SA-4 and SA-8: creates the first admin's login, then the community linked to it.
 * If saving the community fails, the new login is deleted again.
 */
class CreateOrganizationUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val accounts: AdminAccountGateway,
    private val organizations: OrganizationRepository,
    private val activity: ActivityRepository,
) {
    suspend operator fun invoke(details: OrganizationDetails, admin: NewAdmin): Result<String> = domainResult {
        val errors = buildMap<Field, ValidationError> {
            validateDetails(details)
            validateAdmin(admin)
        }
        if (errors.isNotEmpty()) throw DomainException.Validation(errors)
        val actor = auth.currentUser() ?: throw DomainException.NotAuthorized()

        val login = accounts.createLogin(admin.email.trim(), admin.temporaryPassword)
        val orgId = try {
            organizations.create(
                details = details.trimmed(),
                firstAdmin = OrgAdmin(
                    uid = login.uid,
                    orgId = "",
                    name = admin.name.trim(),
                    email = admin.email.trim(),
                    phone = admin.phone.trim(),
                    active = true,
                ),
            )
        } catch (e: Exception) {
            runCatching { login.rollback() }
            throw e
        }
        login.commit()
        runCatching { activity.log(orgId, ActivityAction.ORG_CREATED, actor.uid, details.name.trim()) }
        orgId
    }
}
