# Взаимодействие сервисов в RouteLab Lite

## Общая идея

Платформа состоит из **6 сервисов**. Каждый сервис отвечает за одну чётко ограниченную область.  
Они не знают про детали реализации друг друга — общаются только через **gRPC контракты**.

Снаружи есть только одна точка входа — **API Gateway** на порту 8080.  
Все остальные сервисы недоступны извне — только внутри Docker сети.

```
Внешний мир
    │
    ▼
[API Gateway :8080]  ← единственная публичная точка
    │
    ├──gRPC──► [Tenant Service :9091]
    ├──gRPC──► [Routing Service :9095]
    ├──gRPC──► [ETA Service :9094]
    └──gRPC──► [Live Position Service :9093]
                    │
              [Transit Catalog :9092]  ← только внутренний
```

---

## Роль каждого сервиса

### API Gateway
**Что это:** входная дверь платформы. Принимает HTTP запросы от клиентов.

**Что делает:**
- Проверяет API ключ (делегирует в Tenant Service)
- Считает rate limit (сам, через Redis)
- Проксирует запрос в нужный внутренний сервис через gRPC
- Возвращает ответ клиенту в JSON

**Чего НЕ делает:** не содержит бизнес-логики. Не знает про остановки, маршруты, расписание. Только маршрутизация и безопасность.

---

### Tenant Service
**Что это:** охранник. Знает кто имеет право пользоваться платформой.

**Что делает:**
- Хранит в MySQL список тенантов (компаний-клиентов) и их API ключи
- Отвечает на вопрос: "этот API ключ валидный? Кому он принадлежит? Какой у него лимит?"

**Чего НЕ делает:** не знает про транспорт, маршруты, позиции. Только SaaS-управление.

---

### Transit Catalog Service
**Что это:** библиотека расписаний. Хранит всё статическое.

**Что делает:**
- Хранит в MySQL: остановки, маршруты, рейсы, расписание
- Отвечает на вопросы:
  - "какие остановки есть в радиусе 800м от этой точки?"
  - "какие рейсы едут от остановки A до остановки B после 12:00?"
  - "какое расписание у остановки на сегодня?"

**Чего НЕ делает:** не знает где сейчас находятся автобусы. Только статика.

---

### Live Position Service
**Что это:** диспетчер реального времени. Знает где сейчас каждый автобус.

**Что делает:**
- Принимает позиции автобусов (lat, lon, speed) от внешних источников
- Хранит последнюю позицию каждого автобуса в Redis (TTL 120 сек)
- Вычисляет задержку рейса: насколько автобус опаздывает относительно расписания
- Публикует события в Kafka (для будущих потребителей)

**Чего НЕ делает:** не строит маршруты, не считает ETA для пассажиров.

---

### ETA Service
**Что это:** предсказатель. Знает когда автобус приедет на остановку.

**Что делает:**
- Берёт расписание из Transit Catalog
- Берёт текущую задержку из Live Position
- Считает: `predicted = scheduled + delay`
- Если live данных нет — возвращает расписание как есть (graceful fallback)

**Чего НЕ делает:** не хранит данные. Только агрегирует из двух источников и считает.

---

### Routing Service
**Что это:** навигатор. Строит маршрут от точки А до точки Б.

**Что делает:**
- Находит ближайшие остановки к точке отправления и назначения
- Ищет рейсы между парами остановок
- Для каждого варианта считает полное время: пешком до остановки + ожидание + автобус + пешком от остановки
- Возвращает top-3 варианта отсортированных по времени

**Чего НЕ делает:** не хранит данные. Только алгоритм поверх Catalog и ETA.

---

## Три главных сценария

### Сценарий 1: Клиент строит маршрут

```
Клиент отправляет:
  POST /v1/routes:build
  X-Api-Key: demo-key-1
  { origin: {55.751, 37.618}, destination: {55.761, 37.641}, ... }
```

**Шаг 1 — Аутентификация**
```
API Gateway
  → SHA256("demo-key-1") = "abc123..."
  → Redis: GET apikey:abc123  (кэш)
  → если нет в кэше: TenantService.AuthenticateApiKey("abc123")
  ← { valid: true, tenant_id: "t-1", rpm: 60 }
  → Redis: SET apikey:abc123 {tenant_id, rpm} EX 300
```

**Шаг 2 — Rate limit**
```
API Gateway
  → Redis: INCR tenant:t-1:rpm
  → если count == 1: EXPIRE tenant:t-1:rpm 60
  → если count > 60: вернуть HTTP 429
```

**Шаг 3 — Построение маршрута**
```
API Gateway → RoutingService.BuildRoute(origin, dest, "12:00:00", "2026-05-01", 800m)

RoutingService:
  → CatalogService.GetStopsInRadius(55.751, 37.618, 800m)
  ← [stopA "Арбатская" 320м, stopB "Боровицкая" 650м]

  → CatalogService.GetStopsInRadius(55.761, 37.641, 800m)
  ← [stopX "Тверская" 280м, stopY "Пушкинская" 510м]

  → CatalogService.GetTripsBetweenStops(stopA, stopX, "12:00:00", "2026-05-01")
  ← [{ trip_id: "t-42", route: "24", departure: "12:05:00", arrival: "12:16:00" }]

  → EtaService.PredictArrival("t-42", stopX, "2026-05-01")
      → CatalogService.GetScheduleForStop(stopX, "2026-05-01") → scheduled: "12:16:00"
      → LivePositionService.GetTripDelay("t-42") → delay_sec: 120
      ← predicted: "12:18:00"

  Собираем RouteCandidate:
    WALK  320м → 229 сек (320 / 1.4)
    BUS   wait 300 сек (12:05 - 12:00) + transit 780 сек (12:18 - 12:05)
    WALK  280м → 200 сек
    total = 229 + 300 + 780 + 200 = 1509 сек ≈ 25 мин

API Gateway ← top-3 RouteCandidate
```

**Шаг 4 — Ответ клиенту**
```json
{
  "request_id": "req-abc",
  "routes": [{
    "total_eta_sec": 1509,
    "walking_distance_m": 600,
    "segments": [
      { "type": "WALK", "eta_sec": 229 },
      { "type": "BUS", "route_name": "24", "wait_sec": 300, "eta_sec": 780 },
      { "type": "WALK", "eta_sec": 200 }
    ]
  }]
}
```

---

### Сценарий 2: Диспетчер отправляет позицию автобуса

```
Диспетчер (или GPS трекер на автобусе) отправляет:
  POST /v1/admin/live-positions:ingest
  [{ vehicle_id: "bus-42", trip_id: "t-42", lat: 55.754, lon: 37.622, speed: 8.5, event_time: "..." }]
```

```
API Gateway → LivePositionService.IngestVehiclePosition(position)

LivePositionService:
  1. Дедупликация: проверить не обрабатывали ли уже эту позицию
     (по vehicle_id + event_time)

  2. Сохранить в Redis:
     HSET vehicle:bus-42:pos lat 55.754 lon 37.622 trip_id t-42 event_time ...
     EXPIRE vehicle:bus-42:pos 120

  3. Вычислить задержку:
     Автобус едет со скоростью 8.5 м/с
     До следующей остановки 850м → прибудет через 100 сек
     По расписанию должен был прибыть 80 сек назад → опаздывает на 180 сек

     SET trip:t-42:delay 180
     EXPIRE trip:t-42:delay 120

  4. Добавить в множество активных:
     SADD active-vehicles:moscow-demo bus-42
     EXPIRE active-vehicles:moscow-demo 120

  5. Опубликовать в Kafka:
     topic: vehicle-position-events
     key: "bus-42"
     value: { vehicleId, tripId, lat, lon, speed, eventTime }

← { accepted: true }
```

**Почему Redis с TTL 120 сек?**  
Если автобус перестал отправлять позиции (сломался GPS, конец маршрута) — через 2 минуты его данные автоматически исчезнут из Redis. Система не будет показывать устаревшие позиции.

---

### Сценарий 3: Клиент смотрит прибытия на остановку

```
Клиент запрашивает:
  GET /v1/stops/stop-arb/arrivals?service_day=2026-05-01
  X-Api-Key: demo-key-1
```

```
API Gateway → EtaService.GetStopArrivals("stop-arb", "2026-05-01")

EtaService:
  → CatalogService.GetScheduleForStop("stop-arb", "2026-05-01")
  ← [
      { trip_id: "t-42", route: "24", scheduled_arrival: "12:16:00" },
      { trip_id: "t-55", route: "31", scheduled_arrival: "12:22:00" },
      { trip_id: "t-67", route: "24", scheduled_arrival: "12:31:00" }
    ]

  Для каждого рейса:
  → LivePositionService.GetTripDelay("t-42") ← 180 сек (есть live данные)
  → LivePositionService.GetTripDelay("t-55") ← 0 сек (нет live данных → fallback)
  → LivePositionService.GetTripDelay("t-67") ← 0 сек (нет live данных → fallback)

  Считаем predicted:
    t-42: 12:16:00 + 180 сек = 12:19:00  (опаздывает на 3 мин)
    t-55: 12:22:00 + 0       = 12:22:00  (по расписанию)
    t-67: 12:31:00 + 0       = 12:31:00  (по расписанию)
```

```json
{
  "stop_id": "stop-arb",
  "arrivals": [
    { "route_id": "24", "trip_id": "t-42", "scheduled_at": "12:16:00", "predicted_at": "12:19:00", "delay_sec": 180 },
    { "route_id": "31", "trip_id": "t-55", "scheduled_at": "12:22:00", "predicted_at": "12:22:00", "delay_sec": 0 },
    { "route_id": "24", "trip_id": "t-67", "scheduled_at": "12:31:00", "predicted_at": "12:31:00", "delay_sec": 0 }
  ]
}
```

---

## Почему такое разделение на сервисы?

### Принцип Single Responsibility
Каждый сервис делает одно и хорошо:
- **Catalog** — знает расписание
- **LivePosition** — знает где автобус сейчас
- **ETA** — умеет предсказывать
- **Routing** — умеет строить маршруты
- **Tenant** — управляет доступом
- **Gateway** — управляет трафиком

### Независимое масштабирование
Если много запросов на построение маршрутов — масштабируем только `routing-service`.  
Если много ingest позиций — масштабируем только `live-position-service`.  
Остальные не трогаем.

### Независимые хранилища
| Сервис | Хранилище | Почему |
|---|---|---|
| Tenant | MySQL | транзакционные данные, редко меняются |
| Catalog | MySQL | статика, нужны сложные JOIN запросы |
| LivePosition | Redis | нужна скорость, данные живут 2 минуты |
| ETA | нет | только агрегирует из других |
| Routing | нет | только алгоритм |
| Gateway | Redis | rate limit счётчики, кэш auth |

### Graceful degradation
Если `live-position-service` упал:
- `ETA Service` получает ошибку при `GetTripDelay`
- Возвращает `delay_sec = 0` (fallback на расписание)
- Клиент получает ответ с расписанием вместо live данных
- Система работает, просто менее точно

Если `routing-service` упал:
- `POST /v1/routes:build` возвращает 503
- Остальные endpoints (`/arrivals`, `/vehicles/live`) работают нормально

---

## Что течёт через Kafka

Kafka в MVP используется только как **журнал событий** — `live-position-service` публикует каждую принятую позицию.

```
live-position-service
    │
    └──► Kafka topic: vehicle-position-events
              key: vehicleId
              value: { vehicleId, tripId, lat, lon, speed, eventTime }
```

**Зачем это нужно даже без consumers:**
- Все позиции сохраняются в Kafka (retention 24h)
- В будущем можно добавить consumer для аналитики, ML, replay
- Это стандартный паттерн event sourcing — сначала пишем событие, потом обрабатываем

---

## Жизненный цикл данных

```
Статические данные (расписание):
  seed.sql → MySQL (Catalog) → кэш в Redis (опционально)
  Живут вечно, меняются редко (импорт нового расписания)

Live данные (позиции):
  GPS трекер → ingest API → Redis (TTL 120s) + Kafka
  Живут 2 минуты в Redis, 24 часа в Kafka

Вычисленные данные (ETA, маршруты):
  Считаются на лету при каждом запросе
  Не хранятся (в MVP)

Auth данные:
  MySQL (Tenant) → кэш в Redis (TTL 300s)
  Живут до деактивации ключа
```
