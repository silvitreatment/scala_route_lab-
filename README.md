# RouteLab Lite

Backend SaaS-платформа для построения маршрутов общественного транспорта и прогноза прибытия.

## Стек

- **Scala 2.13** + **ZIO 2.x**
- **Pekko HTTP** — REST API
- **gRPC (ScalaPB)** — межсервисное взаимодействие
- **PostgreSQL 16** — хранение статических данных
- **Redis** — rate limiting, live state транспорта
- **Kafka** — публикация событий позиций транспорта
- **Docker Compose** — локальный запуск

## Сервисы

| Сервис | Порт | Описание |
|---|---|---|
| `api-gateway` | HTTP 8080 | Единая точка входа, auth, rate limit |
| `tenant-service` | gRPC 9091 | Аутентификация API ключей |
| `transit-catalog-service` | gRPC 9092 | Остановки, маршруты, расписание |
| `live-position-service` | gRPC 9093 | Live позиции транспорта, Redis state |
| `eta-service` | gRPC 9094 | Прогноз прибытия на остановку |
| `routing-service` | gRPC 9095 | Построение маршрутов walk+bus+walk |

## Быстрый старт

```bash
# 1. Поднять инфраструктуру и сервисы
docker-compose up -d

# 2. Загрузить демо-данные
psql -h 127.0.0.1 -U postgres -d routelab -f infra/seed.sql
```

## API

### Построить маршрут

```bash
curl -X POST http://localhost:8080/v1/routes:build \
  -H "X-Api-Key: demo-key-1" \
  -H "Content-Type: application/json" \
  -d '{
    "origin": { "lat": 55.751, "lon": 37.618 },
    "destination": { "lat": 55.761, "lon": 37.641 },
    "departure_time": "12:00:00",
    "service_day": "2026-05-01",
    "max_walking_distance_m": 800,
    "region_id": "moscow-demo"
  }'
```

### Прогноз прибытия на остановку

```bash
curl http://localhost:8080/v1/stops/stop-1/arrivals?service_day=2026-05-01 \
  -H "X-Api-Key: demo-key-1"
```

### Отправить live позицию транспорта

```bash
curl -X POST http://localhost:8080/v1/admin/live-positions:ingest \
  -H "X-Api-Key: demo-key-1" \
  -H "Content-Type: application/json" \
  -d '[{
    "vehicle_id": "bus-42",
    "trip_id": "trip-1",
    "lat": 55.754,
    "lon": 37.622,
    "speed": 8.5,
    "event_time": "2026-05-01T09:03:00Z"
  }]'
```

### Активные vehicle

```bash
curl "http://localhost:8080/v1/vehicles/live?region_id=moscow-demo" \
  -H "X-Api-Key: demo-key-1"
```

## Структура проекта

```
routelab/
├── proto/                  # .proto файлы для всех gRPC сервисов
├── shared/                 # Общие domain types + Haversine утилита
├── infra/                  # PostgreSQL DDL (init.sql) и демо-данные (seed.sql)
├── api-gateway/            # Pekko HTTP, auth middleware, rate limit
├── tenant-service/         # Аутентификация API ключей, PostgreSQL
├── transit-catalog-service/# Остановки, маршруты, расписание, PostgreSQL
├── live-position-service/  # Live позиции, Redis state, Kafka producer
├── eta-service/            # Прогноз прибытия (gRPC клиент)
├── routing-service/        # Алгоритм маршрутизации (gRPC клиент)
├── build.sbt               # SBT multi-project build
└── docker-compose.yml      # Локальный запуск всей платформы
```

## Архитектурный план

Подробная архитектура, proto контракты, DDL, Redis ключи и алгоритмы:
[`plans/routelab-architecture.md`](plans/routelab-architecture.md)
