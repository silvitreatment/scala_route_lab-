// Точка входа transit-catalog-service.
// Слои: AppConfig → MySqlTransactor → StopRepo + TripRepo + StopTimeRepo
// → TransitCatalogServiceImpl → GrpcServer.
// gRPC порт 9092.
