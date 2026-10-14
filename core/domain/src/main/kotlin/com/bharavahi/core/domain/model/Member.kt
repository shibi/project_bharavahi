package com.bharavahi.core.domain.model

import java.time.LocalDate

/** A person in the community. Members never log in; admins keep their records. */
data class Member(
    val id: String,
    val name: String,
    val nickname: String,
    val phone: String,
    val email: String,
    val status: MemberStatus,
    val joinedAtMillis: Long?,
    val extras: MemberExtras = MemberExtras(),
)

enum class MemberStatus {
    ACTIVE,

    /** Hidden from pickers, pending lists and the WhatsApp message (BR-10); history is kept. */
    DISABLED,

    /** Left the community. Never hard-deleted (ORG-9). */
    ARCHIVED,
}

/** Input for adding or editing a member. */
data class MemberDetails(
    val name: String,
    val nickname: String,
    val phone: String,
    val email: String,
    val extras: MemberExtras = MemberExtras(),
)

/** ORG-7 "Additional details". Every field is optional. */
data class MemberExtras(
    val dateOfBirth: LocalDate? = null,
    val gender: Gender? = null,
    val bloodGroup: BloodGroup? = null,
    val job: String = "",
    val houseName: String = "",
    val shirtSize: ShirtSize? = null,
) {
    val isEmpty: Boolean
        get() = this == MemberExtras()
}

enum class Gender { MALE, FEMALE, OTHER }

enum class BloodGroup(val label: String) {
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-"),
}

enum class ShirtSize { XS, S, M, L, XL, XXL, XXXL }
