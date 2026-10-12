package com.bharavahi.core.domain.usecase.organization

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.repository.ActivityRepository
import com.bharavahi.core.domain.repository.AuthRepository
import com.bharavahi.core.domain.repository.OrgAdminRepository
import com.bharavahi.core.domain.usecase.domainResult
import javax.inject.Inject

/** SA-5: activate or deactivate one admin of a community. */
class SetAdminActiveUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val admins: OrgAdminRepository,
    private val activity: ActivityRepository,
) {
    suspend operator fun invoke(admin: OrgAdmin, active: Boolean): Result<Unit> = domainResult {
        val actor = auth.currentUser() ?: throw DomainException.NotAuthorized()
        admins.setActive(admin.orgId, admin.uid, active)
        val action = if (active) ActivityAction.ADMIN_ACTIVATED else ActivityAction.ADMIN_DEACTIVATED
        runCatching { activity.log(admin.orgId, action, actor.uid, admin.email) }
        Unit
    }
}
