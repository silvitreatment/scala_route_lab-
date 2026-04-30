// Pekko HTTP route для GET /v1/stops/{stopId}/arrivals
// 1. Извлечь stopId из пути, service_day из query param (default: today)
// 2. AuthMiddleware.authenticate(apiKey)
// 3. RateLimitMiddleware.check(tenantId)
// 4. EtaClient.getStopArrivals(stopId, serviceDay)
// 5. Вернуть JSON с stop_id и arrivals list
