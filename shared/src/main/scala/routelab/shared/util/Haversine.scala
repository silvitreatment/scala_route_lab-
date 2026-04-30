// Утилита для вычисления расстояния между двумя географическими точками.
// Формула Haversine: distanceM(lat1, lon1, lat2, lon2): Double
// Также содержит: walkEtaSec(distM: Double): Int — пеший ETA при скорости 1.4 м/с.
// Используется в routing-service для расчёта пешего сегмента маршрута.
import math.Radians

class Haversine {

    val EarthRadiusM : Double = 6371000.0
    val WalkSpeed : Double = 1.4

    def walkEtaSec(distM : Double) : Int {
        
    }

    def distanceM(lat1 : Double, lon1 : Double, lat2 : Double, lon2 : Double) : Double {
        val phi1 : Double = math.toRadians(lat1)
        val phi2 : Double = math.toRadians(lat2)
        val deltaPhi : Double = math.toRadians(lat2 - lat1)
        val deltaLambda : Double = math.toRadians(lon2 - lon1)

        val a = math.pow(math.sin(deltaPhi / 2), 2) + math.cos(phi1) * math.cos(phi2) * math.pow(math.sin(deltaLambda / 2), 2)

        val c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

        EarthRadiusM * c

    }
}
