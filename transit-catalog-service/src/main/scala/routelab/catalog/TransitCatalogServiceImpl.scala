// Реализация gRPC сервиса TransitCatalogService.
// getStopsInRadius     — делегирует в StopRepo.findInRadius (Haversine SQL)
// getStopById          — делегирует в StopRepo.findById
// getTripsBetweenStops — делегирует в TripRepo.findBetweenStops (JOIN stop_times)
// getScheduleForStop   — делегирует в StopTimeRepo.findByStop
