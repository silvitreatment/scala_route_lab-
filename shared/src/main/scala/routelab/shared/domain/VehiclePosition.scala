package routelab.shared.domain

// Доменная модель live-позиции транспортного средства.
// Поля: vehicleId, tripId, lat, lon, speed, eventTime.
// Используется в live-position-service и api-gateway.

final case class VehiclePosition(
    vehicleId: String,
    tripId: String,
    lat: Double,
    lon: Double,
    speed: Double,
    eventTime: Long,
)
