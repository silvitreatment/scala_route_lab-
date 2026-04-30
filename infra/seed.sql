-- RouteLab Lite — Demo seed data
-- Idempotent: вычищает таблицы и заливает свежий набор.
--
-- Содержит:
--   * 1 tenant (tenant-1, Demo City, rpm=60)
--   * 1 api key (plaintext: "demo-key-1", sha256 ниже)
--   * 30 остановок в регионе moscow-demo
--   * 5 автобусных маршрутов
--   * 50 рейсов на 2026-05-01 (по 10 на маршрут, с шагом 15 минут)
--   * stop_times: 660 записей (по 12..15 остановок на рейс)

BEGIN;

-- Полная очистка (CASCADE через FK)
TRUNCATE stop_times, trips, routes, stops, api_keys, tenants RESTART IDENTITY CASCADE;

-- ============================================================
-- Tenants & API keys
-- ============================================================
INSERT INTO tenants (id, name, status, requests_per_minute) VALUES
  ('tenant-1', 'Demo City', 'active', 60);

-- SHA-256("demo-key-1") = 0b2c109e25ac7d47cc0c56f999832031c7391890ee1893f299b5df9a9256f1d1
INSERT INTO api_keys (id, tenant_id, key_hash, active) VALUES
  ('apikey-1',
   'tenant-1',
   '0b2c109e25ac7d47cc0c56f999832031c7391890ee1893f299b5df9a9256f1d1',
   TRUE);

-- ============================================================
-- Stops: 30 точек около центра Москвы (Красная площадь ~ 55.7539, 37.6208)
-- ============================================================
INSERT INTO stops (id, region_id, name, lat, lon)
SELECT
  'stop-' || LPAD(g::text, 3, '0'),
  'moscow-demo',
  'Demo Stop #' || g,
  -- разнесём координаты по сетке вокруг центра Москвы
  (55.7400 + ((g - 1) % 6) * 0.0040)::numeric(10, 7),
  (37.6000 + ((g - 1) / 6) * 0.0060)::numeric(10, 7)
FROM generate_series(1, 30) AS g;

-- ============================================================
-- Routes: 5 автобусных линий
-- ============================================================
INSERT INTO routes (id, region_id, short_name, transport_type, active) VALUES
  ('route-1', 'moscow-demo', 'B1', 'BUS', TRUE),
  ('route-2', 'moscow-demo', 'B2', 'BUS', TRUE),
  ('route-3', 'moscow-demo', 'B3', 'BUS', TRUE),
  ('route-4', 'moscow-demo', 'B4', 'BUS', TRUE),
  ('route-5', 'moscow-demo', 'B5', 'BUS', TRUE);

-- ============================================================
-- Route → stops mapping (в TEMP таблице)
-- Распределение остановок по маршрутам (по 12..15 шт.)
--   route-1: stops  1..12  (12)
--   route-2: stops  5..17  (13)
--   route-3: stops 10..22  (13)
--   route-4: stops 14..28  (15)
--   route-5: stops 18..30  (13)
-- ============================================================
CREATE TEMP TABLE route_stops (
  route_id VARCHAR(36),
  seq      INTEGER,
  stop_id  VARCHAR(36)
) ON COMMIT DROP;

INSERT INTO route_stops (route_id, seq, stop_id)
SELECT 'route-1', s,      'stop-' || LPAD(s::text, 3, '0') FROM generate_series(1, 12) s;
INSERT INTO route_stops (route_id, seq, stop_id)
SELECT 'route-2', s - 4,  'stop-' || LPAD(s::text, 3, '0') FROM generate_series(5, 17) s;
INSERT INTO route_stops (route_id, seq, stop_id)
SELECT 'route-3', s - 9,  'stop-' || LPAD(s::text, 3, '0') FROM generate_series(10, 22) s;
INSERT INTO route_stops (route_id, seq, stop_id)
SELECT 'route-4', s - 13, 'stop-' || LPAD(s::text, 3, '0') FROM generate_series(14, 28) s;
INSERT INTO route_stops (route_id, seq, stop_id)
SELECT 'route-5', s - 17, 'stop-' || LPAD(s::text, 3, '0') FROM generate_series(18, 30) s;

-- ============================================================
-- Trips: 50 рейсов (по 10 на маршрут) на service_day = 2026-05-01
-- ============================================================
INSERT INTO trips (id, route_id, service_day, direction_id)
SELECT
  'trip-' || r || '-' || LPAD(t::text, 2, '0'),
  'route-' || r,
  DATE '2026-05-01',
  0
FROM generate_series(1, 5) r
CROSS JOIN generate_series(1, 10) t;

-- ============================================================
-- stop_times: для каждого рейса — все остановки его маршрута
--   старт рейса: 06:00 + (t-1) * 15 минут
--   между остановками: ~2 минуты, стоянка ~30 секунд
-- ============================================================
INSERT INTO stop_times (trip_id, stop_id, stop_sequence, scheduled_arrival, scheduled_departure)
SELECT
  'trip-' || r || '-' || LPAD(t::text, 2, '0'),
  rs.stop_id,
  rs.seq,
  (TIME '06:00:00'
     + ((t - 1) * INTERVAL '15 minutes')
     + ((rs.seq - 1) * INTERVAL '2 minutes')
  )::time AS scheduled_arrival,
  (TIME '06:00:00'
     + ((t - 1) * INTERVAL '15 minutes')
     + ((rs.seq - 1) * INTERVAL '2 minutes')
     + INTERVAL '30 seconds'
  )::time AS scheduled_departure
FROM generate_series(1, 5) r
CROSS JOIN generate_series(1, 10) t
JOIN route_stops rs ON rs.route_id = 'route-' || r;

COMMIT;

-- Sanity-check (выведет в psql итоговые количества)
SELECT 'tenants'    AS table, COUNT(*) AS cnt FROM tenants
UNION ALL SELECT 'api_keys',   COUNT(*) FROM api_keys
UNION ALL SELECT 'stops',      COUNT(*) FROM stops
UNION ALL SELECT 'routes',     COUNT(*) FROM routes
UNION ALL SELECT 'trips',      COUNT(*) FROM trips
UNION ALL SELECT 'stop_times', COUNT(*) FROM stop_times;
