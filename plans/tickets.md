# RouteLab Lite — Атомарные тикеты

Порядок: строго сверху вниз. Каждый тикет — одна завершённая единица работы.

---

## Эпик 1: Фундамент (День 1–2)

### RLAB-001 — SBT multi-project build
**Что сделать:**
- Написать корневой `build.sbt` с подпроектами: `shared`, `proto`, `tenant-service`, `transit-catalog-service`, `live-position-service`, `eta-service`, `routing-service`, `api-gateway`
- Объявить версии зависимостей: ZIO 2.1.6, Pekko HTTP 1.0.3, ScalaPB 0.11.17, gRPC 1.64.0, Doobie 1.0.0-RC5, Redis4Cats 1.7.1, fs2-kafka 3.5.1, Circe 0.14.9
- Настроить `project/plugins.sbt`: добавить `sbt-protoc` + `compilerplugin`
- Настроить `project/build.properties`: `sbt.version=1.10.1`
- Проверить: `sbt compile` проходит без ошибок на пустых модулях

**Acceptance criteria:** `sbt projects` показывает все 8 подпроектов

---

### RLAB-002 — Proto codegen pipeline
**Что сделать:**
- Написать все 5 `.proto` файлов: `tenant.proto`, `transit_catalog.proto`, `live_position.proto`, `eta.proto`, `routing.proto`
- Настроить в `build.sbt` для модуля `proto`: `Compile / PB.targets := Seq(scalapb.gen(grpc = true) -> ...)`
- Проверить: `sbt proto/compile` генерирует Scala классы в `target/scala-2.13/src_managed/`

**Acceptance criteria:** все 5 proto компилируются, ScalaPB генерирует trait'ы сервисов и message классы

---

### RLAB-003 — Shared domain types
**Что сделать:**
- Написать case классы в `shared/`: `Stop`, `Route`, `Trip`, `StopTime`, `VehiclePosition`, `Tenant`
- Написать `Haversine.scala`: `distanceM(lat1, lon1, lat2, lon2): Double` и `walkEtaSec(distM: Double): Int`
- Написать `KafkaEvents.scala`: `VehiclePositionEvent` с Circe `@JsonCodec` или ручными encoder/decoder

**Acceptance criteria:** `sbt shared/compile` проходит, Haversine возвращает корректное расстояние для известных координат

---

### RLAB-004 — Docker Compose инфраструктура
**Что сделать:**
- Написать `docker-compose.yml` с сервисами: `postgres`, `redis`, `kafka` (KRaft, без Zookeeper)
- Написать `infra/init.sql`: CREATE TABLE для `tenants`, `api_keys`, `stops`, `routes`, `trips`, `stop_times` с индексами
- Проверить: `docker-compose up postgres redis kafka` поднимается, PostgreSQL принимает соединения, Kafka топик создаётся

**Acceptance criteria:** `docker-compose up -d` без ошибок, `psql -h 127.0.0.1 -U postgres -d routelab -c "\dt"` показывает 6 таблиц

---

### RLAB-005 — Seed демо-данные
**Что сделать:**
- Написать `infra/seed.sql`:
  - 1 tenant: `{ id: "tenant-1", name: "Demo City", rpm: 60 }`
  - 1 api_key: `{ key_hash: SHA256("demo-key-1"), tenant_id: "tenant-1" }`
  - 30 остановок в регионе `moscow-demo` с реальными координатами
  - 5 маршрутов (автобусные линии)
  - 50 рейсов (trips) на дату `2026-05-01`
  - stop_times для каждого рейса (10–15 остановок на рейс)

**Acceptance criteria:** после `psql -h 127.0.0.1 -U postgres -d routelab -f infra/seed.sql` запрос `SELECT COUNT(*) FROM stop_times` возвращает > 400

---

## Эпик 2: Tenant Service (День 2)

### RLAB-006 — PostgresTransactor ZLayer
**Что сделать:**
- Написать `PostgresTransactor.scala` (переиспользуется в tenant и catalog сервисах)
- `HikariTransactor.fromHikariConfig` обёрнутый в `ZLayer`
- Конфиг читается из `AppConfig` (env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`)

**Acceptance criteria:** `ZLayer` компилируется, при неверном URL падает с понятной ошибкой

---

### RLAB-007 — TenantRepo + ApiKeyRepo
**Что сделать:**
- `TenantRepo.findById(id): Task[Option[Tenant]]`
- `ApiKeyRepo.findByHash(keyHash: String): Task[Option[(String, Int)]]` — возвращает `(tenantId, rpm)`
  - SQL: `SELECT t.id, t.requests_per_minute FROM api_keys k JOIN tenants t ON t.id = k.tenant_id WHERE k.key_hash = ? AND k.active = 1`

**Acceptance criteria:** юнит-тест с H2 или интеграционный тест с реальным PostgreSQL возвращает данные из seed

---

### RLAB-008 — TenantServiceImpl gRPC
**Что сделать:**
- Реализовать `TenantServiceImpl extends TenantServiceGrpc.TenantService`
- Метод `authenticateApiKey`: вызвать `ApiKeyRepo.findByHash`, вернуть `AuthenticateApiKeyResponse`
- Написать `GrpcServer.scala`: `ZLayer` с `ServerBuilder`, `acquireRelease`
- Написать `Main.scala`: собрать все слои, запустить

**Acceptance criteria:** `sbt tenant-service/run` стартует, `grpcurl -plaintext localhost:9091 routelab.tenant.TenantService/AuthenticateApiKey` возвращает `valid: true` для demo ключа

---

## Эпик 3: Transit Catalog Service (День 3)

### RLAB-009 — StopRepo
**Что сделать:**
- `findInRadius(lat, lon, radiusM, regionId): Task[List[Stop]]`
  - SQL с Haversine формулой, `HAVING distance_m <= radiusM`, `ORDER BY distance_m LIMIT 10`
- `findById(stopId): Task[Option[Stop]]`

**Acceptance criteria:** запрос с координатами центра Москвы и радиусом 1000м возвращает остановки из seed

---

### RLAB-010 — TripRepo
**Что сделать:**
- `findBetweenStops(fromStopId, toStopId, afterTime, serviceDay, regionId): Task[List[TripCandidate]]`
  - SQL: JOIN trips + routes + stop_times (дважды для from/to), WHERE seq_from < seq_to AND departure >= afterTime, LIMIT 5

**Acceptance criteria:** для двух остановок из seed возвращает хотя бы 1 рейс

---

### RLAB-011 — StopTimeRepo
**Что сделать:**
- `findByStop(stopId, serviceDay): Task[List[StopTimeEntry]]`
  - SQL: JOIN stop_times + trips + routes WHERE stop_id = ? AND service_day = ?, ORDER BY scheduled_arrival

**Acceptance criteria:** для остановки из seed возвращает список рейсов с временами

---

### RLAB-012 — TransitCatalogServiceImpl gRPC
**Что сделать:**
- Реализовать все 4 метода: `getStopsInRadius`, `getStopById`, `getTripsBetweenStops`, `getScheduleForStop`
- Написать `GrpcServer.scala` и `Main.scala`

**Acceptance criteria:** `grpcurl` вызовы всех 4 методов возвращают данные

---

## Эпик 4: Live Position Service (День 4)

### RLAB-013 — RedisClient ZLayer
**Что сделать:**
- Написать `RedisClient.scala` — `ZLayer` с `Redis4Cats` клиентом
- Конфиг: `REDIS_HOST`, `REDIS_PORT` из env

**Acceptance criteria:** `ZLayer` компилируется, при недоступном Redis падает с ошибкой

---

### RLAB-014 — RedisVehicleState
**Что сделать:**
- `savePosition(pos: VehiclePosition): Task[Unit]` — `HSET vehicle:{id}:pos ... EX 120`
- `getPosition(vehicleId): Task[Option[VehiclePosition]]`
- `saveDelay(tripId, delaySec): Task[Unit]` — `SET trip:{tripId}:delay {n} EX 120`
- `getDelay(tripId): Task[Option[Int]]`
- `addToActiveSet(regionId, vehicleId): Task[Unit]` — `SADD active-vehicles:{regionId} {id} + EXPIRE 120`
- `getActiveVehicleIds(regionId): Task[Set[String]]`

**Acceptance criteria:** сохранить позицию → прочитать → получить те же данные

---

### RLAB-015 — VehicleEventProducer (Kafka)
**Что сделать:**
- Написать `VehicleEventProducer.scala` с fs2-kafka
- `publish(event: VehiclePositionEvent): Task[Unit]`
- Сериализация: Circe JSON, ключ = `vehicleId`
- Топик: `vehicle-position-events`
- Конфиг: `KAFKA_BOOTSTRAP` из env

**Acceptance criteria:** после вызова `publish` сообщение видно в `kafka-console-consumer`

---

### RLAB-016 — LivePositionServiceImpl gRPC
**Что сделать:**
- `ingestVehiclePosition`: дедупликация по `vehicleId+eventTime` (проверить Redis), сохранить позицию, вычислить delay (упрощённо: принять из входящих данных или 0), сохранить delay, добавить в active set, опубликовать в Kafka
- `getVehiclePosition`: из Redis
- `getTripDelay`: из Redis
- `getActiveVehicles`: из Redis set → для каждого getPosition
- Написать `GrpcServer.scala` и `Main.scala`

**Acceptance criteria:** ingest позиции → getVehiclePosition возвращает её же; сообщение появляется в Kafka

---

## Эпик 5: ETA Service (День 5, первая половина)

### RLAB-017 — gRPC клиенты в eta-service
**Что сделать:**
- `CatalogClient.scala`: обернуть `TransitCatalogServiceGrpc.stub` в ZIO Task
  - методы: `getScheduleForStop`, `getStopsInRadius`
- `LivePositionClient.scala`: обернуть `LivePositionServiceGrpc.stub`
  - методы: `getTripDelay`
  - при ошибке соединения → вернуть `0` (graceful fallback)

**Acceptance criteria:** клиенты компилируются, при запущенных зависимостях возвращают данные

---

### RLAB-018 — ArrivalPredictor + EtaServiceImpl gRPC
**Что сделать:**
- `ArrivalPredictor.predict(scheduledArrival: LocalTime, delaySec: Int): LocalTime`
- `EtaServiceImpl.predictArrival`: getScheduleForStop → найти entry для tripId → getTripDelay → predict
- `EtaServiceImpl.getStopArrivals`: getScheduleForStop → для каждого entry getTripDelay → predict → отсортировать
- Написать `GrpcServer.scala` и `Main.scala`

**Acceptance criteria:** `grpcurl getStopArrivals` для остановки из seed возвращает список прибытий

---

## Эпик 6: Routing Service (День 5, вторая половина)

### RLAB-019 — gRPC клиенты в routing-service
**Что сделать:**
- `CatalogClient.scala`: `getStopsInRadius`, `getTripsBetweenStops`
- `EtaClient.scala`: `predictArrival`

**Acceptance criteria:** клиенты компилируются

---

### RLAB-020 — NearbyStopsFinder
**Что сделать:**
- `find(point, radiusM, regionId): Task[List[(Stop, Double)]]`
  - вызвать `CatalogClient.getStopsInRadius`
  - обогатить каждую остановку расстоянием через `Haversine.distanceM`

**Acceptance criteria:** для точки из seed возвращает остановки с корректными расстояниями

---

### RLAB-021 — TripCandidateFinder
**Что сделать:**
- `find(originStops, destStops, afterTime, serviceDay, regionId): Task[List[EnrichedCandidate]]`
  - для каждой пары (originStop, destStop): `CatalogClient.getTripsBetweenStops`
  - дедуплицировать по `tripId`
  - `EnrichedCandidate` содержит: `TripCandidate` + `originStop` + `destStop` + `distOrigin` + `distDest`

**Acceptance criteria:** для двух точек из seed возвращает хотя бы 1 кандидата

---

### RLAB-022 — RouteCandidateBuilder
**Что сделать:**
- `build(candidates, origin, destination): Task[List[RouteCandidate]]`
- Для каждого кандидата:
  - `walkToStopSec = Haversine.walkEtaSec(distOrigin)`
  - `waitForBusSec = max(0, secondsBetween(now, scheduledDeparture))`
  - `busSec = EtaClient.predictArrival → secondsBetween(departure, predictedArrival)`
  - `walkFromStopSec = Haversine.walkEtaSec(distDest)`
  - `totalEtaSec = sum всех`
- Сегменты: `[WALK, BUS, WALK]`
- Отсортировать по `totalEtaSec`, вернуть top-3

**Acceptance criteria:** возвращает список из 1–3 маршрутов с корректными сегментами

---

### RLAB-023 — RoutingServiceImpl gRPC
**Что сделать:**
- `buildRoute`: вызвать `NearbyStopsFinder` → `TripCandidateFinder` → `RouteCandidateBuilder`
- Написать `GrpcServer.scala` и `Main.scala`

**Acceptance criteria:** `grpcurl buildRoute` с координатами из seed возвращает маршрут

---

## Эпик 7: API Gateway (День 6)

### RLAB-024 — gRPC клиенты в api-gateway
**Что сделать:**
- `TenantClient.scala`: `authenticateApiKey`
- `RoutingClient.scala`: `buildRoute`
- `EtaClient.scala`: `getStopArrivals`
- `LivePositionClient.scala`: `ingestVehiclePosition`, `getActiveVehicles`

**Acceptance criteria:** все 4 клиента компилируются

---

### RLAB-025 — AuthMiddleware
**Что сделать:**
- `authenticate(request): Task[(String, Int)]` — `(tenantId, rpm)`
  1. Извлечь `X-Api-Key` заголовок → 401 если нет
  2. SHA-256 хэш
  3. Redis GET `apikey:{keyHash}` → если есть, вернуть кэшированные данные
  4. Иначе `TenantClient.authenticateApiKey` → если `valid=false` → 401
  5. Redis HSET `apikey:{keyHash}` EX 300

**Acceptance criteria:** запрос с `demo-key-1` проходит, с неверным ключом → 401

---

### RLAB-026 — RateLimitMiddleware
**Что сделать:**
- `check(tenantId, rpm): Task[Unit]`
  1. `INCR tenant:{tenantId}:rpm`
  2. Если count == 1: `EXPIRE ... 60`
  3. Если count > rpm: вернуть ошибку → 429

**Acceptance criteria:** после N+1 запроса в минуту возвращается HTTP 429

---

### RLAB-027 — RoutesRoute (POST /v1/routes:build)
**Что сделать:**
- Pekko HTTP route
- Распарсить JSON body через Spray JSON или Circe
- Auth + RateLimit
- `RoutingClient.buildRoute` → сформировать JSON ответ
- Обработать ошибки: 400 (невалидный body), 401, 429, 500

**Acceptance criteria:** curl запрос с корректными координатами возвращает маршрут

---

### RLAB-028 — StopsRoute (GET /v1/stops/{stopId}/arrivals)
**Что сделать:**
- Извлечь `stopId` из пути, `service_day` из query (default: today)
- Auth + RateLimit
- `EtaClient.getStopArrivals` → JSON ответ

**Acceptance criteria:** curl запрос для остановки из seed возвращает arrivals

---

### RLAB-029 — VehiclesRoute (GET /v1/vehicles/live)
**Что сделать:**
- Извлечь `region_id` из query param
- Auth + RateLimit
- `LivePositionClient.getActiveVehicles` → JSON ответ

**Acceptance criteria:** после ingest позиций curl возвращает список vehicle

---

### RLAB-030 — AdminRoute (POST /v1/admin/live-positions:ingest)
**Что сделать:**
- Принять JSON массив позиций
- Для каждой: `LivePositionClient.ingestVehiclePosition`
- Вернуть `{ "accepted": N }`
- Только auth, без rate limit

**Acceptance criteria:** curl с массивом позиций → `{ "accepted": N }`, позиции видны в Redis

---

### RLAB-031 — HttpServer + Main (api-gateway)
**Что сделать:**
- Собрать все routes через `~`
- `Http().newServerAt("0.0.0.0", port).bind(routes)` в ZIO resource
- Написать `Main.scala` со всеми слоями

**Acceptance criteria:** `docker-compose up api-gateway` стартует, все 4 endpoint'а отвечают

---

## Эпик 8: Интеграция (День 7)

### RLAB-032 — Docker Compose: все сервисы
**Что сделать:**
- Добавить `Dockerfile` для каждого сервиса (sbt-native-packager или `sbt assembly`)
- Прописать `depends_on`, env vars, healthcheck для PostgreSQL
- Проверить порядок старта: postgres → tenant + catalog → live + eta → routing → gateway

**Acceptance criteria:** `docker-compose up -d` поднимает все 8 контейнеров без ошибок

---

### RLAB-033 — End-to-end сценарий
**Что сделать:**
- Написать `scripts/e2e.sh`:
  1. `psql -h 127.0.0.1 -U postgres -d routelab -f infra/seed.sql`
  2. POST `/v1/admin/live-positions:ingest` с 3 позициями
  3. GET `/v1/vehicles/live?region_id=moscow-demo` → проверить 3 vehicle
  4. GET `/v1/stops/{stopId}/arrivals` → проверить delay
  5. POST `/v1/routes:build` → проверить маршрут с 3 сегментами

**Acceptance criteria:** скрипт выполняется без ошибок, все ответы содержат ожидаемые данные

---

### RLAB-034 — README финальный
**Что сделать:**
- Описание продукта
- Архитектурная схема (ASCII или ссылка на plans/)
- Как поднять: `docker-compose up -d` + seed
- Curl-примеры для всех 4 endpoint'ов
- Описание сервисов и портов

**Acceptance criteria:** новый человек может поднять проект и вызвать все API по README за 10 минут

---

## Итого: 34 тикета

| Эпик | Тикеты | Дни |
|---|---|---|
| Фундамент | RLAB-001 — RLAB-005 | 1–2 |
| Tenant Service | RLAB-006 — RLAB-008 | 2 |
| Transit Catalog | RLAB-009 — RLAB-012 | 3 |
| Live Position | RLAB-013 — RLAB-016 | 4 |
| ETA Service | RLAB-017 — RLAB-018 | 5 (утро) |
| Routing Service | RLAB-019 — RLAB-023 | 5 (вечер) |
| API Gateway | RLAB-024 — RLAB-031 | 6 |
| Интеграция | RLAB-032 — RLAB-034 | 7 |
