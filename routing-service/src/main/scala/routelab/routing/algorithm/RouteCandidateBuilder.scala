// Сборка финальных маршрутов из кандидатов.
// build(candidates, origin, destination): Task[List[RouteCandidate]]
// Для каждого кандидата:
//   walkToStopSec  = Haversine.walkEtaSec(distOriginToOriginStop)
//   waitForBusSec  = max(0, secondsBetween(now, trip.scheduledDeparture))
//   busSec         = EtaClient.predictArrival(tripId, destStopId) - scheduledDeparture
//   walkFromStopSec = Haversine.walkEtaSec(distDestStopToDestination)
//   totalEtaSec    = walkToStopSec + waitForBusSec + busSec + walkFromStopSec
// Сегменты: [WALK(walkToStop), BUS(wait+bus), WALK(walkFromStop)]
