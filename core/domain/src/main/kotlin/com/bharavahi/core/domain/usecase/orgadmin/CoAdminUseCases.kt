package com.bharavahi.core.domain.usecase.orgadmin

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.NewAdmin
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.repository.ActivityRepository
import com.bharavahi.core.domain.repository.AdminAccountGateway
import com.bharavahi.core.domain.repository.AuthRepository
import com.bharavahi.core.domain.repository.OrgAdminRepository
import com.bharavahi.core.domain.repository.OrgContext
import com.bharavahi.core.domain.usecase.domainResult
import com.bharavahi.core.domain.usecase.organization.validateAdmin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** ORG-4: a community has at most this many active admins. */
const val MAX_ACTIVE_ADMINS = 3

/** ORG-3 and ORG-4: every admin of the signed-in admin's community, active or not. */
class ObserveOrgAdminsUseCase @Inject constructor(
    private val org: OrgContext,
    private val admins: OrgAdminRepository,
) {
    operator fun invoke(): Flow<List<OrgAdmin>> = flow { emitAll(admins.observeAdmins(org.orgId())) }
}

/**
 * ORG-4: creates a login for a co-admin and links it to the community. The login is made through
 * the secondary-app gateway (SA-8) so the current admin stays signed in; if linking fails, the
 * new login is deleted again.
 */
class AddCoAdminUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val org: OrgContext,
    private val admins: OrgAdminRepository,
    private val accounts: AdminAccountGateway,
    private val activity: ActivityRepository,
) {
    suspend operator fun invoke(admin: NewAdmin): Result<Unit> = domainResult {
        val errors = buildMap<Field, ValidationError> { validateAdmin(admin) }
        if (errors.isNotEmpty()) throw DomainException.Validation(errors)
        val actor = auth.currentUser() ?: throw DomainException.NotAuthorized()
        val orgId = org.orgId()
        if (admins.observeAdmins(orgId).first().count { it.active } >= MAX_ACTIVE_ADMINS) {
            throw DomainException.AdminLimitReached()
        }

        val email = admin.email.trim()
        val login = accounts.createLogin(email, admin.temporaryPassword)
        try {
            admins.add(
                orgId = orgId,
                admin = OrgAdmin(
                    uid = login.uid,
                    orgId = orgId,
                    name = admin.name.trim(),
                    email = email,
                    phone = admin.phone.trim(),
                    active = true,
                ),
                addedBy = actor.uid,
            )
        } catch (e: Exception) {
            runCatching { login.rollback() }
            throw e
        }
        login.commit()
        runCatching { activity.log(orgId, ActivityAction.ADMIN_ADDED, actor.uid, email) }
        Unit
    }
}

/** ORG-4: activate or deactivate another admin of the community. An admin can't deactivate themselves. */
class SetCoAdminActiveUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val org: OrgContext,
    private val admins: OrgAdminRepository,
    private val activity: ActivityRepository,
) {
    suspend operator fun invoke(admin: OrgAdmin, active: Boolean): Result<Unit> = domainResult {
        val actor = auth.currentUser() ?: throw DomainException.NotAuthorized()
        if (admin.uid == actor.uid) throw DomainException.NotAuthorized()
        val orgId = org.orgId()
        if (active && admins.observeAdmins(orgId).first().count { it.active } >= MAX_ACTIVE_ADMINS) {
            throw DomainException.AdminLimitReached()
        }
        admins.setActive(orgId, admin.uid, active)
        val action = if (active) ActivityAction.ADMIN_ACTIVATED else ActivityAction.ADMIN_DEACTIVATED
        runCatching { activity.log(orgId, action, actor.uid, admin.email) }
        Unit
    }
}
