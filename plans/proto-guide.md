# Proto файлы — назначение и контракты

Все `.proto` файлы лежат в [`proto/`](../proto/).  
ScalaPB генерирует из них Scala код в модуле `proto-gen` при `sbt proto/compile`.  
Каждый сервис зависит от `proto-gen` и использует сгенерированные trait'ы и message классы.

---

## Как это работает в целом

```
proto/*.proto
    ↓ sbt-protoc + ScalaPB
proto-gen/target/.../scalapb/
    ├── routelab.tenant.TenantServiceGrpc        ← trait для impl + stub для клиента
    ├── routelab.catalog.TransitCatalogServiceGrpc
    ├── routelab.liveposition.LivePositionServiceGrpc
    ├── routelab.eta.EtaServiceGrpc
    └── routelab.routing.RoutingServiceGrpc
```

**Сервер** (например `tenant-service`) реализует сгенерированный trait:
```scala
class TenantServiceImpl extends TenantServiceGrpc.TenantService { ... }
```

**Клиент** (например `api-gateway`) использует сгенерированный stub:
```scala
val stub = TenantServiceGrpc.stub(channel)
stub.authenticateApiKey(request)  // возвращает Future[...]
```

---

## `tenant.proto` — аутентификация API ключей

**Файл:** [`proto/tenant.proto`](../proto/tenant.proto)  
**Сервис:** `tenant-service` (порт 9091)  
**Клиенты:** `api-gateway`

### Зачем нужен

API Gateway не хранит информацию о тенантах — он делегирует проверку ключа в Tenant Service по gRPC.  
Это позволяет централизованно управлять ключами и не дублировать логику аутентификации.

### Метод

| Метод | Вход | Выход | Описание |
|---|---|---|---|
| `AuthenticateApiKey` | `key_hash: String` | `valid, tenant_id, rpm` | Проверить API ключ, вернуть политику тенанта |

### Поток данных

```
Client → [X-Api-Key: demo-key-1]
    → API Gateway: SHA256(key) → key_hash
    → TenantService.AuthenticateApiKey(key_hash)
    ← { valid: true, tenant_id: "t-1", rpm: 60 }
```

### Почему только один метод

В MVP не нужны `RegisterUsage` и `CheckQuota` — rate limit делается прямо в Redis в API Gateway, без обращения к Tenant Service на каждый запрос.

---

## `transit_catalog.proto` — статические данные транспорта

**Файл:** [`proto/transit_catalog.proto`](../proto/transit_catalog.proto)  
**Сервис:** `transit-catalog-service` (порт 9092)  
**Клиенты:** `routing-service`, `eta-service`

### Зачем нужен

Хранит расписание, остановки и маршруты в MySQL. Другие сервисы (routing, eta) не имеют прямого доступа к БД — они запрашивают данные через этот gRPC контракт.

### Методы

| Метод | Вход | Выход | Кто вызывает |
|---|---|---|---|
| `GetStopsInRadius` | lat, lon, radius_m, region_id | список `Stop` | `routing-service` |
| `GetStopById` | stop_id | `Stop` | `eta-service` (опционально) |
| `GetTripsBetweenStops` | from_stop_id, to_stop_id, after_time, service_day, region_id | список `TripCandidate` | `routing-service` |
| `GetScheduleForStop` | stop_id, service_day | список `StopTimeEntry` | `eta-service` |

### Ключевые message типы

**`Stop`** — остановка с координатами:
```
stop_id, region_id, name, lat, lon
```

**`TripCandidate`** — рейс между двумя остановками:
```
trip_id, route_id, route_short_name,
scheduled_departure (HH:mm:ss),
scheduled_arrival   (HH:mm:ss)
```

**`StopTimeEntry`** — запись расписания для остановки:
```
trip_id, route_short_name, scheduled_arrival
```

### Поток данных (routing)

```
RoutingService.buildRoute(origin, dest)
    → CatalogClient.GetStopsInRadius(origin, 800m)  ← список остановок рядом
    → CatalogClient.GetStopsInRadius(dest, 800m)
    → CatalogClient.GetTripsBetweenStops(stopA, stopB, "12:00:00", "2026-05-01")
    ← список TripCandidate
```

---

## `live_position.proto` — live позиции транспорта

**Файл:** [`proto/live_position.proto`](../proto/live_position.proto)  
**Сервис:** `live-position-service` (порт 9093)  
**Клиенты:** `api-gateway` (ingest + getActive), `eta-service` (getTripDelay)

### Зачем нужен

Принимает позиции автобусов в реальном времени, хранит их в Redis (TTL 120s) и публикует в Kafka. ETA Service использует текущую задержку рейса для корректировки прогноза прибытия.

### Методы

| Метод | Вход | Выход | Кто вызывает |
|---|---|---|---|
| `IngestVehiclePosition` | `VehiclePosition` | `accepted: bool` | `api-gateway` (admin endpoint) |
| `GetVehiclePosition` | vehicle_id | `VehiclePosition` | `api-gateway` (vehicles/live) |
| `GetTripDelay` | trip_id | `TripDelayState` | `eta-service` |
| `GetActiveVehicles` | region_id | список `VehiclePosition` | `api-gateway` (vehicles/live) |

### Ключевые message типы

**`VehiclePosition`** — текущая позиция:
```
vehicle_id, trip_id, lat, lon, speed, event_time (ISO-8601)
```

**`TripDelayState`** — задержка рейса:
```
trip_id, current_delay_sec
```

### Поток данных (ingest)

```
POST /v1/admin/live-positions:ingest
    → API Gateway
    → LivePositionService.IngestVehiclePosition(position)
        → Redis: HSET vehicle:{id}:pos ...  EX 120
        → Redis: SET trip:{tripId}:delay N  EX 120
        → Redis: SADD active-vehicles:{region} {id}
        → Kafka: vehicle-position-events
    ← { accepted: true }
```

### Поток данных (ETA)

```
EtaService.getStopArrivals(stopId)
    → LivePositionService.GetTripDelay(tripId)
    ← { trip_id: "t-42", current_delay_sec: 120 }
    → predicted = scheduled + 120s
```

---

## `eta.proto` — прогноз прибытия

**Файл:** [`proto/eta.proto`](../proto/eta.proto)  
**Сервис:** `eta-service` (порт 9094)  
**Клиенты:** `api-gateway` (arrivals endpoint), `routing-service`

### Зачем нужен

Инкапсулирует логику предсказания: берёт расписание из Catalog, задержку из LivePosition и возвращает скорректированное время прибытия. Routing Service не знает про Redis и live данные — он просто спрашивает ETA.

### Методы

| Метод | Вход | Выход | Кто вызывает |
|---|---|---|---|
| `PredictArrival` | trip_id, stop_id, service_day | `ArrivalPrediction` | `routing-service` |
| `GetStopArrivals` | stop_id, service_day | список `ArrivalPrediction` | `api-gateway` |

### Ключевые message типы

**`ArrivalPrediction`**:
```
route_short_name, trip_id,
scheduled_at  (HH:mm:ss),
predicted_at  (HH:mm:ss),
delay_sec
```

### Алгоритм внутри ETA Service

```
predicted_at = scheduled_at + delay_sec

если delay_sec = 0 (нет live данных):
    predicted_at = scheduled_at  // fallback на расписание
```

### Поток данных

```
GET /v1/stops/stop-1/arrivals
    → API Gateway
    → EtaService.GetStopArrivals("stop-1", "2026-05-01")
        → CatalogService.GetScheduleForStop("stop-1", "2026-05-01")
        ← [{ trip_id: "t-1", scheduled_arrival: "12:05:00" }, ...]
        → LivePositionService.GetTripDelay("t-1")
        ← { current_delay_sec: 120 }
        → predicted = "12:07:00"
    ← [{ route: "24", scheduled: "12:05", predicted: "12:07", delay: 120 }]
```

---

## `routing.proto` — построение маршрутов

**Файл:** [`proto/routing.proto`](../proto/routing.proto)  
**Сервис:** `routing-service` (порт 9095)  
**Клиенты:** `api-gateway`

### Зачем нужен

Содержит весь алгоритм маршрутизации. API Gateway просто передаёт координаты и получает готовые маршруты — он не знает про остановки, рейсы и ETA.

### Методы

| Метод | Вход | Выход | Кто вызывает |
|---|---|---|---|
| `BuildRoute` | origin, destination, departure_time, service_day, max_walking_m, region_id | список `RouteCandidate` (top-3) | `api-gateway` |

### Ключевые message типы

**`RouteCandidate`** — один вариант маршрута:
```
route_id, total_eta_sec, walking_distance_m,
segments: [RouteSegment]
```

**`RouteSegment`** — один сегмент маршрута:
```
type: WALK | BUS
eta_sec: Int
route_name: String  // только для BUS
wait_sec: Int       // только для BUS
```

### Пример ответа

```json
{
  "routes": [{
    "total_eta_sec": 1260,
    "walking_distance_m": 480,
    "segments": [
      { "type": "WALK", "eta_sec": 240 },
      { "type": "BUS",  "route_name": "24", "wait_sec": 180, "eta_sec": 660 },
      { "type": "WALK", "eta_sec": 180 }
    ]
  }]
}
```

### Алгоритм внутри Routing Service

```
BuildRoute(origin, dest, "12:00:00", "2026-05-01", 800m, "moscow-demo")
    1. CatalogService.GetStopsInRadius(origin, 800m)  → [stopA, stopB, ...]
    2. CatalogService.GetStopsInRadius(dest, 800m)    → [stopX, stopY, ...]
    3. для каждой пары (stopA, stopX):
         CatalogService.GetTripsBetweenStops(stopA, stopX, "12:00:00", ...)
    4. для каждого найденного рейса:
         EtaService.PredictArrival(tripId, stopX)
    5. собрать RouteCandidate:
         WALK(origin→stopA) + BUS(wait+transit) + WALK(stopX→dest)
    6. отсортировать по total_eta_sec, вернуть top-3
```

---

## Зависимости между proto файлами

```
api-gateway
    ├── uses tenant.proto      → AuthenticateApiKey
    ├── uses routing.proto     → BuildRoute
    ├── uses eta.proto         → GetStopArrivals
    └── uses live_position.proto → IngestVehiclePosition, GetActiveVehicles

routing-service
    ├── uses transit_catalog.proto → GetStopsInRadius, GetTripsBetweenStops
    └── uses eta.proto             → PredictArrival

eta-service
    ├── uses transit_catalog.proto → GetScheduleForStop
    └── uses live_position.proto   → GetTripDelay

transit-catalog-service  → implements transit_catalog.proto
tenant-service           → implements tenant.proto
live-position-service    → implements live_position.proto
eta-service              → implements eta.proto
routing-service          → implements routing.proto
```

---

## Почему gRPC, а не REST между сервисами

- **Типизация**: ScalaPB генерирует строго типизированные классы — нет ручного парсинга JSON
- **Производительность**: бинарный протокол (protobuf) быстрее JSON
- **Контракт**: `.proto` файл — единственный источник правды для API между сервисами
- **Codegen**: клиент и сервер генерируются автоматически, нет рассинхронизации
