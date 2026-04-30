// Реализация gRPC сервиса TenantService (ScalaPB trait).
// Метод authenticateApiKey:
//   1. Проверить кэш Redis по ключу apikey:{keyHash} (TTL 300s)
//   2. Если нет — запросить ApiKeyRepo.findByHash
//   3. Если ключ активен — вернуть tenantId + rpm
//   4. Если не найден или неактивен — вернуть valid=false
