// Работа с Redis для хранения live-состояния транспорта.
// savePosition(pos): HSET vehicle:{vehicleId}:pos lat lon trip_id event_time + EXPIRE 120
// getPosition(vehicleId): HGET → Option[VehiclePosition]
// saveDelay(tripId, delaySec): SET trip:{tripId}:delay {delaySec} EX 120
// getDelay(tripId): GET → Option[Int]
// addToActiveSet(regionId, vehicleId): SADD active-vehicles:{regionId} {vehicleId} + EXPIRE 120
// getActiveVehicles(regionId): SMEMBERS → затем getPosition для каждого
