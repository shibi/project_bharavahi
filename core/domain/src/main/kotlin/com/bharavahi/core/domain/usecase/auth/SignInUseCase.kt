package com.bharavahi.core.domain.usecase.auth

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.AuthUser
import com.bharavahi.core.domain.repository.AuthRepository
import com.bharavahi.core.domain.repository.SuperAdminRepository
import com.bharavahi.core.domain.usecase.domainResult
import com.bharavahi.core.domain.validation.Validators
import javax.inject.Inject

/** SA-1: signs in and allows only accounts listed in superAdmins. */
class SignInUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val superAdmins: SuperAdminRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> = domainResult {
        val errors = buildMap<Field, ValidationError> {
            Validators.email(email)?.let { put(Field.EMAIL, it) }
            Validators.required(password)?.let { put(Field.PASSWORD, it) }
        }
        if (errors.isNotEmpty()) throw DomainException.Validation(errors)

        val user = auth.signIn(email.trim(), password)
        if (!superAdmins.isSuperAdmin(user.uid)) {
            auth.signOut()
            throw DomainException.NotAuthorized()
        }
        user
    }
}
