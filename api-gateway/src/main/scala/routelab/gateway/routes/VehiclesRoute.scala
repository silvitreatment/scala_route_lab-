// Pekko HTTP route для GET /v1/vehicles/live
// 1. AuthMiddleware.authenticate(apiKey) → tenantId
// 2. RateLimitMiddleware.check(tenantId)
// 3. Извлечь region_id из query param
// 4. LivePositionClient.getActiveVehicles(regionId)
// 5. Вернуть JSON список активных vehicle с позициями
