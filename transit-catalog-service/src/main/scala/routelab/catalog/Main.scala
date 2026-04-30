// Точка входа transit-catalog-service.
// Слои: AppConfig → PostgresTransactor → StopRepo + TripRepo + StopTimeRepo
// → TransitCatalogServiceImpl → GrpcServer.
// gRPC порт 9092.
