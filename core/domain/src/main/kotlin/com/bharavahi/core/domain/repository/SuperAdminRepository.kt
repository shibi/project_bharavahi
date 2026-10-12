package com.bharavahi.core.domain.repository

interface SuperAdminRepository {
    /** True when superAdmins/{uid} exists. */
    suspend fun isSuperAdmin(uid: String): Boolean
}
