// Middleware для аутентификации по API ключу.
// authenticate(request: HttpRequest): Task[(String, Int)]  // (tenantId, rpm)
//   1. Извлечь заголовок X-Api-Key
//   2. SHA-256 хэш ключа
//   3. Проверить кэш Redis: GET apikey:{keyHash} → если есть, вернуть (tenantId, rpm)
//   4. Если нет — TenantClient.authenticateApiKey(keyHash) gRPC
//   5. Если valid=false → Future.failed(Unauthorized)
//   6. Сохранить в Redis: HSET apikey:{keyHash} tenant_id rpm EX 300
