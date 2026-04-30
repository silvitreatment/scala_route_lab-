// Репозиторий для таблицы stops.
// findInRadius(lat, lon, radiusM, regionId): Task[List[Stop]]
//   — SQL с Haversine формулой, HAVING distance_m <= radiusM, ORDER BY distance_m LIMIT 10
// findById(stopId): Task[Option[Stop]]
