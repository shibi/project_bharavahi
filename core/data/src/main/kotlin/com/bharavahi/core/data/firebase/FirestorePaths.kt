package com.bharavahi.core.data.firebase

/** Collection and field names, matching REQUIREMENTS.md section 6. */
internal object FirestorePaths {
    const val SUPER_ADMINS = "superAdmins"
    const val ADMIN_INDEX = "adminIndex"
    const val ORGANIZATIONS = "organizations"
    const val ADMINS = "admins"
    const val ACTIVITY = "activity"
    const val CONFIG = "config"
    const val MAIL = "mail"

    object Org {
        const val NAME = "name"
        const val TYPE = "type"
        const val ADDRESS = "address"
        const val CONTACT_PHONE = "contactPhone"
        const val STATUS = "status"
        const val MEMBER_COUNT = "memberCount"
        const val EVENT_COUNT = "eventCount"
        const val CREATED_AT = "createdAt"
    }

    object Admin {
        const val NAME = "name"
        const val EMAIL = "email"
        const val PHONE = "phone"
        const val ACTIVE = "active"
        const val ADDED_AT = "addedAt"
        const val ADDED_BY = "addedBy"
    }

    object Index {
        const val ORG_ID = "orgId"
        const val ACTIVE = "active"
    }

    object Activity {
        const val ACTION = "action"
        const val ACTOR_UID = "actorUid"
        const val TARGET = "target"
        const val AT = "at"
    }

    object Mail {
        const val SENDER_EMAIL = "senderEmail"
        const val APP_PASSWORD = "appPassword"
        const val UPDATED_AT = "updatedAt"
    }
}
