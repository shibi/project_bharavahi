package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.ActivityLog
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.model.OrganizationStatus
import com.google.firebase.firestore.DocumentSnapshot

internal fun OrganizationStatus.toFirestore(): String = name.lowercase()

internal fun String?.toOrganizationStatus(): OrganizationStatus =
    OrganizationStatus.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: OrganizationStatus.ACTIVE

internal fun ActivityAction.toFirestore(): String = name.lowercase()

internal fun String?.toActivityAction(): ActivityAction =
    ActivityAction.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: ActivityAction.UNKNOWN

internal fun DocumentSnapshot.toOrganization(): Organization = Organization(
    id = id,
    name = getString(FirestorePaths.Org.NAME).orEmpty(),
    type = getString(FirestorePaths.Org.TYPE).orEmpty(),
    address = getString(FirestorePaths.Org.ADDRESS).orEmpty(),
    contactPhone = getString(FirestorePaths.Org.CONTACT_PHONE).orEmpty(),
    status = getString(FirestorePaths.Org.STATUS).toOrganizationStatus(),
    memberCount = getLong(FirestorePaths.Org.MEMBER_COUNT) ?: 0L,
    eventCount = getLong(FirestorePaths.Org.EVENT_COUNT) ?: 0L,
    createdAtMillis = getTimestamp(FirestorePaths.Org.CREATED_AT)?.toDate()?.time,
)

internal fun DocumentSnapshot.toOrgAdmin(orgId: String): OrgAdmin = OrgAdmin(
    uid = id,
    orgId = orgId,
    name = getString(FirestorePaths.Admin.NAME).orEmpty(),
    email = getString(FirestorePaths.Admin.EMAIL).orEmpty(),
    phone = getString(FirestorePaths.Admin.PHONE).orEmpty(),
    active = getBoolean(FirestorePaths.Admin.ACTIVE) ?: false,
)

/** Activity docs live at organizations/{orgId}/activity/{id}, so the orgId is the grandparent. */
internal fun DocumentSnapshot.toActivityLog(): ActivityLog = ActivityLog(
    id = id,
    orgId = reference.parent.parent?.id.orEmpty(),
    action = getString(FirestorePaths.Activity.ACTION).toActivityAction(),
    actorUid = getString(FirestorePaths.Activity.ACTOR_UID).orEmpty(),
    target = getString(FirestorePaths.Activity.TARGET).orEmpty(),
    atMillis = getTimestamp(FirestorePaths.Activity.AT)?.toDate()?.time,
)
