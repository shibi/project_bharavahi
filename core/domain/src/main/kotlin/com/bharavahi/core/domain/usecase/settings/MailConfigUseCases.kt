package com.bharavahi.core.domain.usecase.settings

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.MailConfig
import com.bharavahi.core.domain.repository.MailConfigRepository
import com.bharavahi.core.domain.usecase.domainResult
import com.bharavahi.core.domain.validation.Validators
import javax.inject.Inject

/** SA-9: read the Gmail sender used for payment backup emails. */
class GetMailConfigUseCase @Inject constructor(private val repository: MailConfigRepository) {
    suspend operator fun invoke(): Result<MailConfig?> = domainResult { repository.get() }
}

/** SA-9: save the Gmail sender. Google app passwords are 16 letters, often shown in groups of 4. */
class SaveMailConfigUseCase @Inject constructor(private val repository: MailConfigRepository) {
    suspend operator fun invoke(senderEmail: String, appPassword: String): Result<Unit> = domainResult {
        val password = appPassword.replace(" ", "")
        val errors = buildMap<Field, ValidationError> {
            Validators.email(senderEmail)?.let { put(Field.SENDER_EMAIL, it) }
            Validators.required(password)?.let { put(Field.APP_PASSWORD, it) }
        }
        if (errors.isNotEmpty()) throw DomainException.Validation(errors)
        repository.save(MailConfig(senderEmail.trim(), password))
    }
}
