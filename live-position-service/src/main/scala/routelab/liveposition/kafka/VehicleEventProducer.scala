// Kafka producer для топика vehicle-position-events.
// Использует fs2-kafka.
// publish(event: VehiclePositionEvent): Task[Unit]
//   — сериализует в JSON через Circe
//   — ключ сообщения: vehicleId (для партиционирования)
//   — топик: vehicle-position-events
