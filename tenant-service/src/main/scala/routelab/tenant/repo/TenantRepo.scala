// Репозиторий для работы с таблицей tenants.
// Методы:
//   findById(id: String): Task[Option[Tenant]]
//   create(tenant: Tenant): Task[Unit]
// Использует Doobie + HikariCP transactor для PostgreSQL.
