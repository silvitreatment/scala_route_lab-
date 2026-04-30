package routelab.shared.domain

// case class — это неизменяемый (immutable) класс данных в Scala.
// Компилятор автоматически генерирует для него:
//   - equals / hashCode (сравнение по значению полей, а не по ссылке)
//   - toString (Stop(stop-1, moscow, Арбатская, 55.75, 37.61))
//   - copy (создать копию с изменёнными полями)
//   - apply в companion object (можно создавать без new)
//   - unapply (можно использовать в pattern matching)
//
// Пример использования:
//   val stop = Stop("stop-1", "moscow", "Арбатская", 55.751, 37.618)
//   val moved = stop.copy(lat = 55.760)  // новый объект с изменённым lat
//   stop == moved  // false (разные lat)

final case class Stop(
  id:       String,  // уникальный идентификатор остановки, например "stop-42"
  regionId: String,  // регион, к которому принадлежит остановка, например "moscow-demo"
  name:     String,  // человекочитаемое название, например "Арбатская"
  lat:      Double,  // широта (latitude), например 55.751244
  lon:      Double   // долгота (longitude), например 37.618423
)

// companion object — объект с тем же именем что и класс.
// Живёт рядом с классом в том же файле.
// Используется для:
//   - фабричных методов (альтернативные конструкторы)
//   - констант, связанных с типом
//   - implicit значений (например, JSON кодеки)
//
// В данном случае companion object пустой — case class уже даёт нам
// Stop.apply("id", "region", "name", lat, lon) бесплатно.
object Stop
