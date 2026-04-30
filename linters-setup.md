# Linters & Formatter Setup

Настройка Scalafmt + Scalafix для проекта routelab.

---

## Быстрый старт

```bash
# Перейти в директорию проекта
cd scala_route_lab-

# Форматировать все .scala файлы
sbt scalafmtAll

# Проверить форматирование без изменений (для CI)
sbt scalafmtCheck

# Применить правила Scalafix (линтер + авто-рефакторинг)
sbt "scalafix"

# Только проверка Scalafix без изменений (для CI)
sbt "scalafix --check"
```

---

## Автоформат в VSCode

**Cmd+S** → Metals автоматически применяет Scalafmt.

Требования:
- Расширение [Metals](https://marketplace.visualstudio.com/items?itemName=scalameta.metals) установлено
- После открытия проекта нажать **"Import build"** в уведомлении Metals

---

## Java Runtime

JDK 21 (Temurin) установлен через Coursier и прописан в `~/.zshrc`:

```zsh
export JAVA_HOME="$HOME/Library/Caches/Coursier/arc/https/github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.11%252B10/OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.11_10.tar.gz/jdk-21.0.11+10/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

Проверить:
```bash
java -version
# openjdk version "21.0.11" ...
```

Применить в текущем терминале (если только что изменили `.zshrc`):
```bash
source ~/.zshrc
```

---

## Файлы конфигурации

| Файл | Назначение |
|------|-----------|
| `.scalafmt.conf` | Правила форматирования (отступы, выравнивание, trailing commas) |
| `.scalafix.conf` | Правила линтера (запрет `var`/`null`/`return`, организация импортов) |
| `project/plugins.sbt` | sbt-плагины: `sbt-scalafmt`, `sbt-scalafix` |
| `.vscode/settings.json` | `formatOnSave` для Scala через Metals |

---

## Что проверяет Scalafix

| Правило | Что запрещает / делает |
|---------|----------------------|
| `DisableSyntax` | `var`, `null`, `return`, `while`, `asInstanceOf`, `isInstanceOf`, `;` |
| `OrganizeImports` | Сортирует и удаляет неиспользуемые импорты |
| `ExplicitResultTypes` | Требует явные типы у публичных/protected методов и val |
| `NoValInForComprehension` | Запрет `val` внутри `for`-comprehension |
| `LeakingImplicitClassVal` | Запрет утечки `val` из implicit class |
| `RedundantSyntax` | Удаляет лишний синтаксис |
| `ProcedureSyntax` | Заменяет `def foo() { }` на `def foo(): Unit = { }` |

---

## Что делает Scalafmt

| Настройка | Значение |
|-----------|---------|
| `maxColumn` | 100 символов |
| `indent.main` | 2 пробела |
| `align.preset` | `more` — выравнивание `=>`, `%%`, `->`, `<-`, `=` |
| `trailingCommas` | `always` — запятая после последнего элемента |
| `rewrite.rules` | `RedundantBraces`, `RedundantParens`, `PreferCurlyFors`, `SortModifiers` |
