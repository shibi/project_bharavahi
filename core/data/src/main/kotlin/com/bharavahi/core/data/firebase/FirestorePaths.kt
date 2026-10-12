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
    const val MEMBERS = "members"
    const val EVENTS = "events"
    const val EVENT_MEMBERS = "eventMembers"
    const val SCHEDULES = "schedules"

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

    object Member {
        const val NAME = "name"
        const val NICKNAME = "nickname"
        const val PHONE = "phone"
        const val EMAIL = "email"
        const val STATUS = "status"
        const val JOINED_AT = "joinedAt"
    }

    object Event {
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val START_DATE = "startDate"
        const val END_DATE = "endDate"
        const val OPENING_BALANCE = "openingBalance"
        const val STATUS = "status"
        const val ROLES = "roles"
        const val TOTAL_COLLECTED = "totalCollected"
        const val TOTAL_EXPENSES = "totalExpenses"
        const val CREATED_AT = "createdAt"
        const val CREATED_BY = "createdBy"
    }

    object Role {
        const val ID = "id"
        const val NAME = "name"
    }

    object EventMember {
        const val NAME = "name"
        const val ROLE_ID = "roleId"
        const val ACTIVE = "active"
        const val TOTAL_PAID = "totalPaid"
        const val PAID_BY_DUE = "paidByDue"
        const val ADDED_AT = "addedAt"
    }

    object Schedule {
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val AMOUNT_PER_MEMBER = "amountPerMember"
        const val NUMBER_OF_DUES = "numberOfDues"
        const val START_DATE = "startDate"
        const val END_DATE = "endDate"
        const val DUES = "dues"
        const val LOCKED = "locked"
        const val CREATED_AT = "createdAt"
    }

    object Due {
        const val INDEX = "index"
        const val LABEL = "label"
        const val DUE_DATE = "dueDate"
        const val AMOUNT = "amount"
    }
}
