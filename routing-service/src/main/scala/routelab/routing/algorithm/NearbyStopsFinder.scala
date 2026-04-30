// Поиск ближайших остановок к точке.
// find(point: LatLon, radiusM: Int, regionId: String): Task[List[(Stop, Double)]]
//   — вызывает CatalogClient.getStopsInRadius
//   — возвращает список (Stop, distanceM) отсортированный по расстоянию
// distanceM вычисляется через Haversine.distanceM из shared модуля.
