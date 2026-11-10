# КБЖУ Трекер

Android-приложение для подсчёта калорий, белков, жиров и углеводов.
Kotlin · Jetpack Compose · Material 3 · MVVM + Clean Architecture · Room · Hilt · Coroutines/Flow ·
CameraX + ML Kit · Retrofit + Open Food Facts · Coil · DataStore. minSdk 26, targetSdk 36.

## Возможности

- Онбординг: пол, возраст, рост, вес → цель → желаемый вес и темп → активность → БЖУ → итог.
  Расчёт по Миффлину — Сан-Жеору (BMR → TDEE → дефицит/профицит), безопасный минимум, ручная корректировка.
- Дневник: кольцо калорий, прогресс БЖУ, 4 приёма пищи, свайп дат и календарь, свайп записей (удалить с отменой / изменить), вода.
- База продуктов: 280 встроенных продуктов, поиск с подсказками, категории, «Недавние», «Избранное», «Мои»,
  онлайн-поиск в Open Food Facts, свои продукты, рецепты из ингредиентов, порции с живым пересчётом.
- Сканер штрих-кодов (EAN-13, EAN-8, UPC): кэш в Room, фонарик, ручной ввод, обработка отказа в доступе к камере.
- Фото продуктов и записей (камера / галерея), сжатие во внутреннее хранилище.
- Статистика за неделю/месяц: калории по дням, средние БЖУ, график веса, история отметок.
- Светлая/тёмная тема, русская локализация, работа офлайн (сеть — только для Open Food Facts).

## Структура

```
app/src/main/java/ru/kbzhu/tracker/
├── KbzhuApp.kt, MainActivity.kt      — Hilt-приложение, Coil, выбор стартового экрана
├── core/util/                        — даты, нормализация строк для поиска
├── domain/                           — чистый Kotlin: модели, интерфейсы, калькулятор, use cases
│   ├── model/  repository/  calculator/  usecase/
├── data/                             — реализация: Room, DataStore, Retrofit, фото
│   ├── local/ (entity, dao, seed)  datastore/  remote/ (dto)  storage/  mapper/  repository/
├── di/                               — Hilt-модули: БД, сеть, привязка репозиториев
└── ui/
    ├── theme/  components/  util/  navigation/
    ├── onboarding/  diary/  foods/  scanner/  stats/  profile/
app/src/main/res/values/strings*.xml  — все строки интерфейса (по экранам)
app/src/test/                         — unit-тест калькулятора нормы
```

## Сборка и запуск

### 1. Что нужно
- **Android Studio** последней стабильной версии (Narwhal 2025.1 или новее) — в ней уже есть JDK 17+.
- Android SDK Platform **36** (Studio предложит установить сама).
- Телефон с Android 8.0+ или эмулятор. Для сканера лучше реальный телефон: у эмулятора виртуальная камера.

### 2. Открыть проект
1. Распакуйте архив. В корне должны лежать `settings.gradle.kts` и папка `app`.
2. Android Studio → **File → Open** → выберите папку `KbzhuTracker` (не `app`!).
3. Дождитесь **Gradle Sync** (первый раз 3–10 минут: скачиваются Gradle 8.13 и библиотеки).
   Файла `gradlew`/`gradle-wrapper.jar` в архиве нет — Studio создаёт и использует wrapper сама
   по `gradle/wrapper/gradle-wrapper.properties`. Для сборки из терминала один раз выполните
   `gradle wrapper --gradle-version 8.13` (если Gradle установлен) — появится `./gradlew`.

### 3. Запустить
- **Телефон:** Настройки → О телефоне → 7 раз нажать «Номер сборки» → «Для разработчиков» →
  включить «Отладка по USB». Подключить кабелем, разрешить отладку.
- **Эмулятор:** Device Manager → Create Device → Pixel 8 → образ API 35/36.
- Выбрать устройство вверху и нажать ▶ **Run 'app'**.

### 4. Проверить калькулятор
Откройте `app/src/test/.../NutritionCalculatorTest.kt` → правый клик → **Run** — 5 тестов должны пройти.

### 5. Собрать APK для установки
- Отладочный: **Build → Build App Bundle(s) / APK(s) → Build APK(s)** → `app/build/outputs/apk/debug/app-debug.apk`.
- Релизный (R8, сжатие): `./gradlew assembleRelease`. Сейчас релиз подписан debug-ключом —
  для Google Play создайте свой: **Build → Generate Signed App Bundle / APK** и пропишите
  `signingConfigs` в `app/build.gradle.kts` (пароли — в `local.properties` или переменных окружения, не в Git).

## Частые проблемы

| Симптом | Решение |
|---|---|
| `SDK location not found` | Studio создаст `local.properties` сама; иначе впишите `sdk.dir=/путь/к/Android/Sdk` |
| `Unsupported class file major version` / ошибка JDK | Settings → Build Tools → Gradle → Gradle JDK = **JetBrains Runtime 17+** (embedded) |
| Studio предлагает обновить AGP/библиотеки | Сначала соберите как есть — версии подобраны совместимыми. Обновляйте по одной, затем Sync |
| `compileSdk 36 requires newer AGP` | Обновите Android Studio; в `libs.versions.toml` можно поднять `agp` |
| KSP/Hilt: `[Hilt] ... missing` после правок | **Build → Clean Project**, затем **Rebuild** |
| Сканер показывает чёрный экран на эмуляторе | Это виртуальная камера. Проверяйте на телефоне или вводите код вручную (кнопка «клавиатура») |
| «Нет подключения к интернету» при скане | Нужна сеть только для первого поиска кода; найденные товары потом работают офлайн |
| Поменяли схему БД (entity) и приложение падает | Увеличьте `version` в `AppDatabase` и добавьте `Migration`, либо на время разработки удалите приложение с устройства |

## Архитектура в двух словах

UI (Compose) → ViewModel (StateFlow состояния + Channel разовых событий) → UseCase → Repository (интерфейс в domain)
→ реализация в data (Room / DataStore / Retrofit / файлы). Hilt связывает интерфейсы с реализациями
в `di/RepositoryModule.kt`. Записи дневника хранят копию КБЖУ — правка или удаление продукта не ломают историю.

Данные Open Food Facts распространяются по лицензии ODbL; источник указан в разделе «О приложении».
