package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.MailConfig

interface MailConfigRepository {
    suspend fun get(): MailConfig?
    suspend fun save(config: MailConfig)
}
