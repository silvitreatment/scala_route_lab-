// Точка входа api-gateway.
// Слои: AppConfig → RedisClient → TenantClient + RoutingClient + EtaClient + LivePositionClient
// → AuthMiddleware + RateLimitMiddleware → HttpServer.
// HTTP порт 8080. Единственный сервис с Pekko HTTP.
