// Реализация gRPC сервиса RoutingService.
// buildRoute(origin, destination, departureTime, serviceDay, maxWalkingM, regionId):
//   1. NearbyStopsFinder.find(origin, maxWalkingM, regionId) → originStops
//   2. NearbyStopsFinder.find(destination, maxWalkingM, regionId) → destStops
//   3. TripCandidateFinder.find(originStops, destStops, departureTime, serviceDay, regionId)
//   4. RouteCandidateBuilder.build(candidates, origin, destination) → List[RouteCandidate]
//   5. Отсортировать по totalEtaSec, вернуть top-3
