package routelab.shared.domain

// Доменная модель тенанта (B2B клиента платформы).
// Поля: id, name, status, requestsPerMinute.

final case class Tenant(
    id: String,
    name: String,
    status: String,
    requestsPerMinute: Int,
)
