// gRPC клиент для transit-catalog-service.
// Оборачивает ScalaPB stub в ZIO Task.
// Методы: getStopsInRadius, getTripsBetweenStops.
// Адрес: CATALOG_GRPC_HOST:CATALOG_GRPC_PORT из AppConfig.
