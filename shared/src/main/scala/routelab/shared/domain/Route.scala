// Доменная модель маршрута (линии) транспорта.
// Поля: id, regionId, shortName, transportType (BUS), active.

final case class Route(
    id: String,
    regionId: String,
    shortName: String,
    transportType: String,
    active: Boolean,
)
