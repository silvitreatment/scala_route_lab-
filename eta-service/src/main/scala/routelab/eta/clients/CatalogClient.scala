// gRPC клиент для transit-catalog-service.
// Оборачивает ScalaPB stub в ZIO Task.
// Методы: getScheduleForStop, getStopsInRadius, getTripsBetweenStops.
// Адрес сервиса берётся из AppConfig (CATALOG_GRPC_HOST, CATALOG_GRPC_PORT).
