// Pekko HTTP route для POST /v1/routes:build
// 1. Распарсить JSON body (origin, destination, departure_time, preferences)
// 2. AuthMiddleware.authenticate(apiKey) → tenantId
// 3. RateLimitMiddleware.check(tenantId) → 429 если превышен
// 4. RoutingClient.buildRoute(request) → RouteCandidate list
// 5. Вернуть JSON ответ с request_id и routes
