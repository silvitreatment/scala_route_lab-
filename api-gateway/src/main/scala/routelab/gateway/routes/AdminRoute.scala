// Pekko HTTP routes для admin endpoints (без rate limit, только auth):
// POST /v1/admin/live-positions:ingest
//   — принять JSON массив VehiclePosition
//   — для каждого вызвать LivePositionClient.ingestVehiclePosition gRPC
//   — вернуть { accepted: N }
// POST /v1/admin/transit-data:import
//   — принять JSON с stops/routes/trips/stop_times
//   — TODO: в MVP можно использовать seed.sql напрямую
