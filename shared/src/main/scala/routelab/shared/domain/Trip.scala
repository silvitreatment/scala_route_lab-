// Доменная модель рейса (конкретного выезда по маршруту в день).
// Поля: id, routeId, serviceDay, directionId.


final case class Trip(
    id : String, 
    routeId : String, 
    serviceDay : String, 
    directionId : String
)