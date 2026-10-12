package com.bharavahi.core.domain.usecase

import com.bharavahi.core.domain.error.DomainException
import kotlin.coroutines.cancellation.CancellationException

/** Runs [block] and wraps failures as [DomainException], keeping coroutine cancellation intact. */
internal suspend inline fun <T> domainResult(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: DomainException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(DomainException.Unknown(e))
    }
