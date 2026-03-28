# ktor-ddd

Lightweight DDD building blocks for Ktor + Koin applications. Provides the core abstractions — `Entity`, `AggregateRoot`, `DomainEvent`, `UnitOfWork`, `EventBus` — plus a Ktor plugin that wires domain event handlers at startup.

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.jwillert:ktor-ddd:0.1.0")
}
```

## Core Abstractions

### Entity

Base class for objects with identity. Equality is based on `id`, not on field values.

```kotlin
@JvmInline
value class UserId(val value: UUID)

class User(id: UserId, var name: String) : Entity<UserId>(id)

val a = User(UserId(uuid), "Alice")
val b = User(UserId(uuid), "Alice renamed")
a == b  // true — same id
```

### AggregateRoot

Use `AggregateRootDelegate` to implement `AggregateRoot` via Kotlin delegation. Call `registerEvent()` inside your aggregate methods to collect events, then dispatch them after the transaction commits.

```kotlin
class Order private constructor(
    id: OrderId,
    val customerId: CustomerId,
    var status: OrderStatus,
) : Entity<OrderId>(id), AggregateRoot by AggregateRootDelegate() {

    companion object {
        fun place(customerId: CustomerId): Order {
            val order = Order(OrderId(UUID.randomUUID()), customerId, OrderStatus.PENDING)
            order.registerEvent(OrderPlaced(order.id, Clock.System.now()))
            return order
        }
    }

    fun confirm() {
        check(status == OrderStatus.PENDING) { "Only pending orders can be confirmed" }
        status = OrderStatus.CONFIRMED
        registerEvent(OrderConfirmed(id, Clock.System.now()))
    }

    fun cancel() {
        check(status != OrderStatus.CANCELLED) { "Order is already cancelled" }
        status = OrderStatus.CANCELLED
        registerEvent(OrderCancelled(id, Clock.System.now()))
    }
}
```

After persisting the aggregate, dispatch and clear its events:

```kotlin
val order = orderRepository.findById(id)
order.confirm()
orderRepository.save(order)

eventBus.publish(order.domainEvents)
order.clearEvents()
```

### DomainEvent

```kotlin
data class OrderConfirmed(
    val orderId: OrderId,
    override val occurredAt: Instant,
) : DomainEvent
```

### DomainException

```kotlin
// Throw from the domain layer — catch in your Ktor status pages
throw NotFoundException("Order ${id.value} not found")
throw ValidationException("Quantity must be positive")
```

Map them in Ktor's `StatusPages`:

```kotlin
install(StatusPages) {
    exception<NotFoundException> { call, ex ->
        call.respond(HttpStatusCode.NotFound, ex.message ?: "Not found")
    }
    exception<ValidationException> { call, ex ->
        call.respond(HttpStatusCode.UnprocessableEntity, ex.message ?: "Validation error")
    }
}
```

### UnitOfWork

`UnitOfWork` is a simple interface for wrapping a block in a transaction. Use `ktor-ddd-exposed` for the Exposed implementation.

```kotlin
interface UnitOfWork {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}
```

Usage in an application service:

```kotlin
class ConfirmOrderService(
    private val orders: OrderRepository,
    private val uow: UnitOfWork,
    private val eventBus: EventBusSender,
) {
    suspend fun confirm(id: OrderId) = uow {
        val order = orders.findById(id) ?: throw NotFoundException("Order $id not found")
        order.confirm()
        orders.save(order)
        eventBus.publish(order.domainEvents)
        order.clearEvents()
    }
}
```

---

## EventBus + DomainEvents Plugin

### EventBus

`EventBus` is an in-process pub/sub bus. Register handlers by event type, then publish a list of events.

```kotlin
// Register via Koin
single { EventBus() }
bind<EventBusSender>() with { get<EventBus>() }
```

### DomainEvents Plugin

Registers event handlers at application startup using a DSL that gives you full Koin access inside each handler.

```kotlin
install(DomainEvents) {
    handlers {
        on<OrderConfirmed> {
            val notifier by inject<NotificationService>()
            notifier.sendConfirmation(event.orderId)
        }

        on<OrderCancelled> {
            val inventory by inject<InventoryService>()
            inventory.releaseReservation(event.orderId)
        }
    }
}
```

`EventHandlerContext` gives you access to the event itself and Koin DI:

| Property / Function | Description |
|---|---|
| `event` | The domain event instance |
| `inject<T>()` | Lazy Koin dependency |
| `get<T>()` | Eager Koin dependency |

---

## Requirements

- Kotlin 2.1+
- Ktor 3.0+
- Koin 3.5+
- JVM 17+
