package com.bharavahi.core.domain.model

/** A community on the platform, e.g. a residents association. */
data class Organization(
    val id: String,
    val name: String,
    val type: String,
    val address: String,
    val contactPhone: String,
    val status: OrganizationStatus,
    val memberCount: Long,
    val eventCount: Long,
    val createdAtMillis: Long?,
)

enum class OrganizationStatus {
    ACTIVE,
    SUSPENDED,

    /** Soft-deleted: hidden from lists, all admins locked out. Data is kept. */
    DELETED,
}

/** Input for creating or editing a community's details. */
data class OrganizationDetails(
    val name: String,
    val type: String,
    val address: String,
    val contactPhone: String,
)
