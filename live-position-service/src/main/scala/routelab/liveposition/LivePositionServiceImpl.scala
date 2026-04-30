// Реализация gRPC сервиса LivePositionService.
// ingestVehiclePosition:
//   1. Дедупликация: проверить event_time в Redis, пропустить если уже обработано
//   2. RedisVehicleState.savePosition(position)
//   3. DelayCalculator.compute(position) → delaySec
//   4. RedisVehicleState.saveDelay(tripId, delaySec)
//   5. RedisVehicleState.addToActiveSet(regionId, vehicleId)
//   6. VehicleEventProducer.publish(position)
// getVehiclePosition  — RedisVehicleState.getPosition(vehicleId)
// getTripDelay        — RedisVehicleState.getDelay(tripId)
// getActiveVehicles   — RedisVehicleState.getActiveVehicles(regionId)
