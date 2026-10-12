package com.bharavahi.core.domain.model

/** A person who can log in to the Organization app for exactly one community. */
data class OrgAdmin(
    val uid: String,
    val orgId: String,
    val name: String,
    val email: String,
    val phone: String,
    val active: Boolean,
)

/** Input for a new admin login, created by the super admin. */
data class NewAdmin(
    val name: String,
    val email: String,
    val phone: String,
    val temporaryPassword: String,
)
