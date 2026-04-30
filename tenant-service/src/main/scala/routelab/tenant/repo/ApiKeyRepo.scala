// Репозиторий для работы с таблицей api_keys.
// Методы:
//   findByHash(keyHash: String): Task[Option[(ApiKey, Tenant)]]
//   create(apiKey: ApiKey): Task[Unit]
// JOIN с таблицей tenants для получения rpm лимита.
