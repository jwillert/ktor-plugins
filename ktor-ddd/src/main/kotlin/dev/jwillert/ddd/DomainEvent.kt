package dev.jwillert.ddd

import kotlin.time.Instant

interface DomainEvent {
    val occurredAt: Instant
}
