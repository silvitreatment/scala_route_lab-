// Логика предсказания времени прибытия.
// predict(scheduledArrival: LocalTime, delaySec: Int): LocalTime
//   — scheduledArrival.plusSeconds(delaySec)
// Если delaySec = 0 (нет live данных) — возвращает scheduledArrival без изменений.
