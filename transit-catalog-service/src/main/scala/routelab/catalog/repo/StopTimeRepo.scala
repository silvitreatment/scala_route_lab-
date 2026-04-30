// Репозиторий для таблицы stop_times.
// findByStop(stopId, serviceDay): Task[List[StopTimeEntry]]
//   — JOIN stop_times + trips + routes WHERE stop_id = ? AND service_day = ?
//   — ORDER BY scheduled_arrival
