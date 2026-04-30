package routelab.shared.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HaversineSpec extends AnyWordSpec with Matchers {

  // Допустимая погрешность для расстояний (0.1% от результата или 1 метр)
  private val EpsilonM = 1.0

  // ── distanceM ────────────────────────────────────────────────

  "Haversine.distanceM" should {

    "return 0 for identical coordinates" in {
      Haversine.distanceM(55.7558, 37.6173, 55.7558, 37.6173) shouldBe 0.0 +- EpsilonM
    }

    "calculate distance between Moscow and Saint Petersburg (~634 km)" in {
      // Москва: 55.7558° N, 37.6173° E
      // Санкт-Петербург: 59.9343° N, 30.3351° E
      val dist = Haversine.distanceM(55.7558, 37.6173, 59.9343, 30.3351)
      // Известное расстояние ~634 км
      dist shouldBe 634_000.0 +- 5_000.0
    }

    "calculate distance between two nearby stops (~500 m)" in {
      // Две точки в Москве на расстоянии ~500 м
      val dist = Haversine.distanceM(55.7500, 37.6200, 55.7545, 37.6200)
      dist shouldBe 500.0 +- 10.0
    }

    "be symmetric: distanceM(A, B) == distanceM(B, A)" in {
      val lat1 = 55.7558
      val lon1 = 37.6173
      val lat2 = 59.9343
      val lon2 = 30.3351
      val ab = Haversine.distanceM(lat1, lon1, lat2, lon2)
      val ba = Haversine.distanceM(lat2, lon2, lat1, lon1)
      ab shouldBe ba +- EpsilonM
    }

    "handle coordinates on the equator" in {
      // Две точки на экваторе, разница 1° долготы.
      // При R = 6371000 м: 2π * 6371000 / 360 ≈ 111194.9 м
      val dist = Haversine.distanceM(0.0, 0.0, 0.0, 1.0)
      dist shouldBe 111_195.0 +- 100.0
    }

    "handle coordinates crossing the prime meridian" in {
      // Точки по обе стороны от нулевого меридиана
      val dist = Haversine.distanceM(51.5, -0.1, 51.5, 0.1)
      dist shouldBe 13_800.0 +- 200.0
    }

    "handle antipodal points (maximum distance ~20015 km)" in {
      // Антиподы: (0, 0) и (0, 180)
      val dist = Haversine.distanceM(0.0, 0.0, 0.0, 180.0)
      dist shouldBe 20_015_000.0 +- 10_000.0
    }

    "return positive distance for negative latitude difference" in {
      val dist = Haversine.distanceM(59.9343, 30.3351, 55.7558, 37.6173)
      dist should be > 0.0
    }
  }

  // ── walkEtaSec ───────────────────────────────────────────────

  "Haversine.walkEtaSec" should {

    "return 0 for zero distance" in {
      Haversine.walkEtaSec(0.0) shouldBe 0
    }

    "calculate ETA for 140 m at 1.4 m/s → 100 sec" in {
      // 140 / 1.4 = 100.0 → toInt = 100
      Haversine.walkEtaSec(140.0) shouldBe 100
    }

    "calculate ETA for 1000 m at 1.4 m/s → 714 sec" in {
      // 1000 / 1.4 = 714.28... → toInt = 714
      Haversine.walkEtaSec(1000.0) shouldBe 714
    }

    "truncate (not round) fractional seconds" in {
      // 1.0 / 1.4 = 0.714... → toInt = 0
      Haversine.walkEtaSec(1.0) shouldBe 0
      // 1.39 / 1.4 = 0.992... → toInt = 0
      Haversine.walkEtaSec(1.39) shouldBe 0
      // 1.4 / 1.4 = 1.0 → toInt = 1
      Haversine.walkEtaSec(1.4) shouldBe 1
    }

    "be consistent with WalkSpeed constant" in {
      val dist = Haversine.WalkSpeed * 60 // 60 секунд ходьбы
      Haversine.walkEtaSec(dist) shouldBe 60
    }
  }

  // ── constants ────────────────────────────────────────────────

  "Haversine constants" should {

    "have EarthRadiusM equal to 6371000.0" in {
      Haversine.EarthRadiusM shouldBe 6_371_000.0
    }

    "have WalkSpeed equal to 1.4 m/s" in {
      Haversine.WalkSpeed shouldBe 1.4
    }
  }
}
