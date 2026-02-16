package com.neomud.server.game

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

object GameStateLock {
    val mutex = Mutex()

    /** Count of times a caller had to suspend because the mutex was already held. */
    val contentionCount = AtomicLong(0)

    /** Wraps [Mutex.withLock], incrementing [contentionCount] when contention occurs. */
    suspend inline fun <T> withLock(crossinline action: suspend () -> T): T {
        val wasLocked = mutex.isLocked
        if (wasLocked) contentionCount.incrementAndGet()
        return mutex.withLock { action() }
    }
}
