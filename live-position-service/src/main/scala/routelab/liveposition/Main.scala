// Точка входа live-position-service.
// Слои: AppConfig → RedisClient + KafkaProducer → RedisVehicleState
// → DelayCalculator → LivePositionServiceImpl → GrpcServer.
// gRPC порт 9093. Зависимости: Redis, Kafka.
