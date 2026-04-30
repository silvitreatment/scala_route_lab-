// Middleware для rate limiting по tenantId.
// check(tenantId: String, rpm: Int): Task[Unit]
//   1. INCR tenant:{tenantId}:rpm
//   2. Если count == 1: EXPIRE tenant:{tenantId}:rpm 60
//   3. Если count > rpm: Task.fail(TooManyRequests)
// Использует Redis4Cats для атомарных операций.
