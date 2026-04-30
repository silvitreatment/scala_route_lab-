CREATE TABLE IF NOT EXISTS tenants (
  id                  VARCHAR(36)  PRIMARY KEY,
  name                VARCHAR(255) NOT NULL,
  status              VARCHAR(20)  NOT NULL DEFAULT 'active',
  requests_per_minute INTEGER      NOT NULL DEFAULT 60,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS api_keys (
  id         VARCHAR(36) PRIMARY KEY,
  tenant_id  VARCHAR(36) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  key_hash   VARCHAR(64) NOT NULL UNIQUE,
  active     BOOLEAN     NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_api_keys_tenant ON api_keys(tenant_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_active_hash ON api_keys(active, key_hash);

CREATE TABLE IF NOT EXISTS stops (
  id         VARCHAR(36)   PRIMARY KEY,
  region_id  VARCHAR(36)   NOT NULL,
  name       VARCHAR(255)  NOT NULL,
  lat        NUMERIC(10,7) NOT NULL,
  lon        NUMERIC(10,7) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_stops_region ON stops(region_id);
CREATE INDEX IF NOT EXISTS idx_stops_lat_lon ON stops(lat, lon);

CREATE TABLE IF NOT EXISTS routes (
  id             VARCHAR(36) PRIMARY KEY,
  region_id      VARCHAR(36) NOT NULL,
  short_name     VARCHAR(50) NOT NULL,
  transport_type VARCHAR(20) NOT NULL DEFAULT 'BUS',
  active         BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_routes_region ON routes(region_id);

CREATE TABLE IF NOT EXISTS trips (
  id           VARCHAR(36) PRIMARY KEY,
  route_id     VARCHAR(36) NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
  service_day  DATE        NOT NULL,
  direction_id SMALLINT    NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_trips_route ON trips(route_id);
CREATE INDEX IF NOT EXISTS idx_trips_service_day ON trips(service_day);

CREATE TABLE IF NOT EXISTS stop_times (
  id                  BIGSERIAL   PRIMARY KEY,
  trip_id             VARCHAR(36) NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
  stop_id             VARCHAR(36) NOT NULL REFERENCES stops(id) ON DELETE CASCADE,
  stop_sequence       INTEGER     NOT NULL,
  scheduled_arrival   TIME        NOT NULL,
  scheduled_departure TIME        NOT NULL,
  CONSTRAINT uq_trip_stop_seq UNIQUE (trip_id, stop_sequence)
);
CREATE INDEX IF NOT EXISTS idx_st_trip ON stop_times(trip_id);
CREATE INDEX IF NOT EXISTS idx_st_stop ON stop_times(stop_id);
CREATE INDEX IF NOT EXISTS idx_st_stop_arrival ON stop_times(stop_id, scheduled_arrival);
