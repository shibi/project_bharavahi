package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.AdminLink
import kotlinx.coroutines.flow.Flow

/** Reads adminIndex/{uid}: which community a login belongs to. */
interface AdminIndexRepository {
    suspend fun find(uid: String): AdminLink?
    fun observe(uid: String): Flow<AdminLink?>
}
