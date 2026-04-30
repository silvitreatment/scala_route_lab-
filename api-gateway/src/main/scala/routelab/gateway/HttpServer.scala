// Pekko HTTP сервер.
// Собирает все Route через ~ оператор:
//   RoutesRoute ~ StopsRoute ~ VehiclesRoute ~ AdminRoute
// Запускается через Http().newServerAt("0.0.0.0", port).bind(routes).
// Мост ZIO ↔ Future: использует ZIO.runtime для unsafeRunToFuture в route handlers.
