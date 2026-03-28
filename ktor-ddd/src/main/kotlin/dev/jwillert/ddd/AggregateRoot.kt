package dev.jwillert.ddd

interface AggregateRoot {
    val domainEvents: List<DomainEvent>
    fun clearEvents()
    fun registerEvent(event: DomainEvent)
}

class AggregateRootDelegate : AggregateRoot {
    private val _events = mutableListOf<DomainEvent>()
    override val domainEvents: List<DomainEvent> get() = _events.toList()
    override fun clearEvents() = _events.clear()
    override fun registerEvent(event: DomainEvent) { _events.add(event) }
}
