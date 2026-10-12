package com.bharavahi.core.domain.model

/** A person in the community. Members never log in; admins keep their records. */
data class Member(
    val id: String,
    val name: String,
    val nickname: String,
    val phone: String,
    val email: String,
    val status: MemberStatus,
    val joinedAtMillis: Long?,
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
)
