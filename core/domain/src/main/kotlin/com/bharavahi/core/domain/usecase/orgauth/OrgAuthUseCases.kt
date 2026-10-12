package com.bharavahi.core.domain.usecase.orgauth

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.AdminSessionState
import com.bharavahi.core.domain.model.AuthUser
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.domain.model.SessionState
import com.bharavahi.core.domain.repository.AdminIndexRepository
import com.bharavahi.core.domain.repository.AuthRepository
import com.bharavahi.core.domain.repository.OrganizationRepository
import com.bharavahi.core.domain.usecase.domainResult
import com.bharavahi.core.domain.validation.Validators
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** ORG-1: signs a community admin in. Refused when the admin link is missing or inactive, or the community is suspended. */
class OrgSignInUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val adminIndex: AdminIndexRepository,
    private val organizations: OrganizationRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> = domainResult {
        val errors = buildMap<Field, ValidationError> {
            Validators.email(email)?.let { put(Field.EMAIL, it) }
            Validators.required(password)?.let { put(Field.PASSWORD, it) }
        }
        if (errors.isNotEmpty()) throw DomainException.Validation(errors)

        val user = auth.signIn(email.trim(), password)
        val allowed = try {
            val link = adminIndex.find(user.uid)
            // Security rules hide a suspended community from its admins, so a refused read also means "not allowed".
            link != null && link.active &&
                organizations.observe(link.orgId).first()?.status == OrganizationStatus.ACTIVE
        } catch (e: DomainException.NotAuthorized) {
            false
        }
        if (!allowed) {
            auth.signOut()
            throw DomainException.NotAuthorized()
        }
        user
    }
}

/** ORG-2: the signed-in admin and their community. Signs out at once if the admin is deactivated. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveAdminSessionUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val adminIndex: AdminIndexRepository,
) {
    operator fun invoke(): Flow<AdminSessionState> = auth.session.flatMapLatest { state ->
        when (state) {
            SessionState.Loading -> flowOf(AdminSessionState.Loading)
            SessionState.SignedOut -> flowOf(AdminSessionState.SignedOut)
            is SessionState.SignedIn -> adminIndex.observe(state.user.uid)
                .map { link ->
                    if (link != null && link.active) {
                        AdminSessionState.SignedIn(state.user, link.orgId)
                    } else {
                        auth.signOut()
                        AdminSessionState.SignedOut
                    }
                }
                .catch {
                    auth.signOut()
                    emit(AdminSessionState.SignedOut)
                }
        }
    }
}
