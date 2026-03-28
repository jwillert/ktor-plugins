# ktor-ddd-exposed

Exposed ORM backend for `ktor-ddd`. Provides `ExposedUnitOfWork`, which wraps a block in a `suspendTransaction`.

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.jwillert:ktor-ddd:0.1.0")
    implementation("dev.jwillert:ktor-ddd-exposed:0.1.0")
}
```

## Usage

### Register with Koin

```kotlin
single<UnitOfWork> { ExposedUnitOfWork() }
```

### Use in application services

`ExposedUnitOfWork` wraps the block in a `suspendTransaction`, so all repository calls inside share the same database transaction.

```kotlin
class PlaceOrderService(
    private val orders: OrderRepository,
    private val uow: UnitOfWork,
    private val eventBus: EventBusSender,
) {
    suspend fun place(customerId: CustomerId): OrderId = uow {
        val order = Order.place(customerId)
        orders.save(order)
        eventBus.publish(order.domainEvents)
        order.clearEvents()
        order.id
    }
}
```

If the block throws, the transaction is rolled back and the exception is rethrown.

## Requirements

- `ktor-ddd:0.1.0`
- Exposed 1.1+
- JVM 17+
