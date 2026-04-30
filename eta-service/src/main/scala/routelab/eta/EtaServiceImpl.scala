// Реализация gRPC сервиса EtaService.
// predictArrival(tripId, stopId, serviceDay):
//   1. CatalogClient.getScheduleForStop → найти запись для tripId
//   2. LivePositionClient.getTripDelay(tripId) → delaySec (0 если нет данных)
//   3. predicted = scheduled + delaySec
//   4. Вернуть ArrivalPrediction
// getStopArrivals(stopId, serviceDay):
//   1. CatalogClient.getScheduleForStop → список StopTimeEntry
//   2. Для каждого entry: LivePositionClient.getTripDelay → delaySec
//   3. Собрать список ArrivalPrediction, отсортировать по predicted_at
