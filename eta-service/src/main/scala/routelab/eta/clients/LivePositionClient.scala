// gRPC клиент для live-position-service.
// Оборачивает ScalaPB stub в ZIO Task.
// Методы: getTripDelay, getVehiclePosition.
// При ошибке соединения возвращает delaySec = 0 (graceful fallback).
