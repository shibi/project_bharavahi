package com.bharavahi.core.domain.usecase.member

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.Member
import com.bharavahi.core.domain.model.MemberDetails
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.repository.MemberRepository
import com.bharavahi.core.domain.repository.OrgContext
import com.bharavahi.core.domain.usecase.domainResult
import com.bharavahi.core.domain.validation.Validators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import javax.inject.Inject

/** ORG-6: all members of the signed-in admin's community. */
class ObserveMembersUseCase @Inject constructor(
    private val org: OrgContext,
    private val members: MemberRepository,
) {
    operator fun invoke(): Flow<List<Member>> = flow { emitAll(members.observeAll(org.orgId())) }

    companion object {
        /** Search by name, nickname or phone; [status] null means every status. */
        fun filter(members: List<Member>, query: String, status: MemberStatus?): List<Member> {
            val q = query.trim()
            val digits = q.filter(Char::isDigit)
            return members.filter { m ->
                (status == null || m.status == status) &&
                    (
                        q.isEmpty() ||
                            m.name.contains(q, ignoreCase = true) ||
                            m.nickname.contains(q, ignoreCase = true) ||
                            (digits.isNotEmpty() && m.phone.filter(Char::isDigit).contains(digits))
                        )
            }
        }
    }
}

class ObserveMemberUseCase @Inject constructor(
    private val org: OrgContext,
    private val members: MemberRepository,
) {
    operator fun invoke(memberId: String): Flow<Member?> = flow { emitAll(members.observe(org.orgId(), memberId)) }
}

/** ORG-7. Returns the new member id. */
class AddMemberUseCase @Inject constructor(
    private val org: OrgContext,
    private val members: MemberRepository,
) {
    suspend operator fun invoke(details: MemberDetails, today: LocalDate = LocalDate.now()): Result<String> = domainResult {
        validate(details, today)
        members.add(org.orgId(), details.trimmed())
    }
}

/** ORG-8. */
class UpdateMemberUseCase @Inject constructor(
    private val org: OrgContext,
    private val members: MemberRepository,
) {
    suspend operator fun invoke(
        memberId: String,
        details: MemberDetails,
        today: LocalDate = LocalDate.now(),
    ): Result<Unit> = domainResult {
        validate(details, today)
        members.update(org.orgId(), memberId, details.trimmed())
    }
}

/** ORG-9: enable, disable or archive. Members are never deleted. */
class SetMemberStatusUseCase @Inject constructor(
    private val org: OrgContext,
    private val members: MemberRepository,
) {
    suspend operator fun invoke(memberId: String, status: MemberStatus): Result<Unit> = domainResult {
        members.setStatus(org.orgId(), memberId, status)
    }
}

private fun validate(details: MemberDetails, today: LocalDate) {
    val errors = buildMap<Field, ValidationError> {
        Validators.required(details.name)?.let { put(Field.NAME, it) }
        Validators.optionalPhone(details.phone)?.let { put(Field.PHONE, it) }
        if (details.email.isNotBlank()) Validators.email(details.email)?.let { put(Field.EMAIL, it) }
        if (details.extras.dateOfBirth?.isAfter(today) == true) put(Field.DATE_OF_BIRTH, ValidationError.FUTURE_DATE)
    }
    if (errors.isNotEmpty()) throw DomainException.Validation(errors)
}

private fun MemberDetails.trimmed() = copy(
    name = name.trim(),
    nickname = nickname.trim(),
    phone = phone.trim(),
    email = email.trim(),
    extras = extras.copy(job = extras.job.trim(), houseName = extras.houseName.trim()),
)
