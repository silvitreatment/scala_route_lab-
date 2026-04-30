// Доменная модель записи расписания: рейс + остановка + время.
// Поля: id, tripId, stopId, stopSequence, scheduledArrival, scheduledDeparture.

final case class StopTime(
    id: String,
    tripId: String,
    stopId: String,
    stopSequence: Int,
    scheduledArrival: String,
    scheduledDeparture: String,
)
