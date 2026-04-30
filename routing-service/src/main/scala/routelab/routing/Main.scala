// Точка входа routing-service.
// Слои: AppConfig → CatalogClient + EtaClient → NearbyStopsFinder
// + TripCandidateFinder + RouteCandidateBuilder → RoutingServiceImpl → GrpcServer.
// gRPC порт 9095. Нет своей БД — только gRPC клиенты.
