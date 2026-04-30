// Репозиторий для таблицы trips.
// findBetweenStops(fromStopId, toStopId, afterTime, serviceDay, regionId): Task[List[TripCandidate]]
//   — JOIN trips + routes + stop_times (дважды: для from и to stop)
//   — WHERE stop_sequence(from) < stop_sequence(to) AND departure >= afterTime
//   — LIMIT 5
