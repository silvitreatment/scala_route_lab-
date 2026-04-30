// Конфигурация сервиса eta-service.
// Читает переменные окружения через ZIO System или typesafe config.
// Поля зависят от сервиса, например:
//   grpcPort: Int          — порт gRPC сервера
//   dbUrl/dbUser/dbPass    — для сервисов с MySQL
//   redisHost/redisPort    — для сервисов с Redis
//   kafkaBootstrap         — для live-position-service
//   *GrpcHost/*GrpcPort    — адреса зависимых gRPC сервисов
