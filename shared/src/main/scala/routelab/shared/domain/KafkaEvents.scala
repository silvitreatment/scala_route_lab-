// Case классы для Kafka событий, сериализуемые через Circe JSON.
// VehiclePositionEvent: vehicleId, tripId, lat, lon, speed, eventTime.
// Ключ сообщения в Kafka: vehicleId (для партиционирования по vehicle).
