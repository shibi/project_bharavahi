package com.bharavahi.core.domain.model

/** One audit-trail entry under a community. */
data class ActivityLog(
    val id: String,
    val orgId: String,
    val action: ActivityAction,
    val actorUid: String,
    val target: String,
    val atMillis: Long?,
)

enum class ActivityAction {
    ORG_CREATED,
    ORG_UPDATED,
    ORG_SUSPENDED,
    ORG_REACTIVATED,
    ORG_DELETED,
    ADMIN_ACTIVATED,
    ADMIN_DEACTIVATED,
    UNKNOWN,
}
