package routelab.shared.domain

// Case классы для Kafka событий, сериализуемые через Circe JSON.
// Топик: vehicle-position-events
// Ключ сообщения в Kafka: vehicleId (для партиционирования по vehicle).

// VehiclePositionEvent — событие, которое live-position-service публикует в Kafka
// каждый раз, когда получает новую GPS-позицию от транспортного средства.
// Поля идентичны VehiclePosition, но это отдельный тип — event ≠ domain entity.
final case class VehiclePositionEvent(
    vehicleId: String, // ID транспортного средства (ключ Kafka-сообщения)
    tripId: String,    // ID рейса, которому принадлежит ТС в данный момент
    lat: Double,       // Широта (градусы)
    lon: Double,       // Долгота (градусы)
    speed: Double,     // Скорость (м/с)
    eventTime: Long,   // Unix timestamp в миллисекундах (System.currentTimeMillis)
)
