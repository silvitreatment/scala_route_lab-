// Точка входа eta-service.
// Слои: AppConfig → CatalogClient (gRPC stub) + LivePositionClient (gRPC stub)
// → ArrivalPredictor → EtaServiceImpl → GrpcServer.
// gRPC порт 9094. Нет своей БД — только gRPC клиенты.
