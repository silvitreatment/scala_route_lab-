// gRPC клиент для eta-service.
// Оборачивает ScalaPB stub в ZIO Task.
// Методы: predictArrival(tripId, stopId, serviceDay).
// Адрес: ETA_GRPC_HOST:ETA_GRPC_PORT из AppConfig.
