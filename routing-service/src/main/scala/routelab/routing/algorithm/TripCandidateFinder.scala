// Поиск рейсов между парами остановок.
// find(originStops, destStops, afterTime, serviceDay, regionId): Task[List[TripCandidate]]
//   — для каждой пары (originStop, destStop) вызывает CatalogClient.getTripsBetweenStops
//   — дедуплицирует по tripId (один рейс может покрывать несколько пар)
//   — возвращает плоский список кандидатов с привязкой к originStop и destStop
