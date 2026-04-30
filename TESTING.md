# Testing Guide

Руководство по запуску тестов в проекте routelab.

---

## Требования

- **Java 21** (Temurin) — прописан в `~/.zshrc`, проверить: `java -version`
- **sbt** — установлен через Coursier, проверить: `sbt --version`

Все команды запускать из директории `scala_route_lab-/`.

---

## Запуск тестов

### Все тесты во всех модулях

```bash
sbt test
```

### Тесты конкретного модуля

```bash
sbt "shared/test"
sbt "tenantService/test"
sbt "transitCatalogService/test"
sbt "livePositionService/test"
sbt "etaService/test"
sbt "routingService/test"
sbt "apiGateway/test"
```

### Один конкретный тест-класс

```bash
sbt "shared/testOnly routelab.shared.util.HaversineSpec"
```

### Тесты по паттерну имени

```bash
# Запустить все Spec-классы в shared
sbt "shared/testOnly *Spec"

# Запустить только тесты с "distance" в названии
sbt "shared/testOnly routelab.shared.util.HaversineSpec -- -z distance"
```

### Непрерывный запуск при изменении файлов (watch mode)

```bash
sbt "~shared/test"
```

Тесты будут перезапускаться автоматически при каждом сохранении `.scala` файла.

---

## Структура тестов

```
shared/
└── src/
    ├── main/scala/routelab/shared/
    │   └── util/
    │       └── Haversine.scala          # исходный код
    └── test/scala/routelab/shared/
        └── util/
            └── HaversineSpec.scala      # тесты
```

Тесты располагаются в `src/test/scala/` и зеркалируют структуру пакетов из `src/main/scala/`.

---

## Фреймворк: ScalaTest

Используется стиль **WordSpec** + **Matchers**:

```scala
class HaversineSpec extends AnyWordSpec with Matchers {
  "Haversine.distanceM" should {
    "return 0 for identical coordinates" in {
      Haversine.distanceM(55.7, 37.6, 55.7, 37.6) shouldBe 0.0 +- 1.0
    }
  }
}
```

### Полезные матчеры

| Матчер | Пример |
|--------|--------|
| Точное равенство | `result shouldBe 42` |
| С допуском (Double) | `result shouldBe 100.0 +- 0.5` |
| Больше / меньше | `result should be > 0.0` |
| Тип | `result shouldBe a [String]` |
| Исключение | `an [IllegalArgumentException] should be thrownBy { ... }` |

---

## Покрытие тестами

### `HaversineSpec` — 15 тестов

| Группа | Тест |
|--------|------|
| `distanceM` | одинаковые координаты → 0 |
| `distanceM` | Москва → Санкт-Петербург (~634 км) |
| `distanceM` | две соседние остановки (~500 м) |
| `distanceM` | симметричность A↔B |
| `distanceM` | точки на экваторе (1° = 111 195 м) |
| `distanceM` | пересечение нулевого меридиана |
| `distanceM` | антиподы (~20 015 км) |
| `distanceM` | отрицательная разница широт → положительный результат |
| `walkEtaSec` | нулевое расстояние → 0 |
| `walkEtaSec` | 140 м → 100 сек |
| `walkEtaSec` | 1000 м → 714 сек |
| `walkEtaSec` | усечение (не округление) дробных секунд |
| `walkEtaSec` | согласованность с константой `WalkSpeed` |
| constants | `EarthRadiusM == 6_371_000.0` |
| constants | `WalkSpeed == 1.4` |

---

## CI

Тесты автоматически запускаются в GitHub Actions при каждом `push` и `pull_request`.

Workflow: [`.github/workflows/lint.yml`](.github/workflows/lint.yml)

Чтобы добавить тесты в CI, добавь шаг в `lint.yml`:

```yaml
- name: Run tests
  run: sbt test
```

---

## Добавление новых тестов

1. Создай файл в `<module>/src/test/scala/<package>/` с суффиксом `Spec`
2. Унаследуй от `AnyWordSpec with Matchers`
3. Запусти: `sbt "<module>/testOnly <FullClassName>"`
