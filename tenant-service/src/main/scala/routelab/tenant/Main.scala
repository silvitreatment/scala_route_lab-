// Точка входа tenant-service.
// Запускает ZIO приложение, собирает слои:
// AppConfig.live → MySqlTransactor.live → TenantRepo.live + ApiKeyRepo.live
// → TenantServiceImpl.live → GrpcServer.live
// Слушает gRPC на порту из переменной окружения GRPC_PORT (по умолчанию 9091).
