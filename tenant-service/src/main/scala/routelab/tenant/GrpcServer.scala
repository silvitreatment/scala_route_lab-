// ZIO resource, оборачивающий io.grpc.Server.
// Регистрирует TenantServiceImpl как gRPC handler.
// При старте биндит порт, при завершении вызывает server.shutdown().
// Предоставляет метод awaitTermination: UIO[Unit] для блокировки Main.
