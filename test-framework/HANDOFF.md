# Передача работы: тестовый фреймворк PetClinic

Документ для нового чата. Здесь итоги аудита, все фазы, состояние репозитория, принятые решения и открытые вопросы.
Отложенные задачи — в `test-framework/BACKLOG.md`. Промпт для нового чата — в `test-framework/NEXT_SESSION_PROMPT.md`.

## 1. Текущее состояние

- Ветка `main`, последний коммит — см. `git log -1` (на момент записи 1e48333 + коммиты документации этого шага).
- CI: Fast Checks — зелёный, Terraform Validation — зелёный (прогон 43865507289 на 1e48333).
- E2E: **31 зелёный, 3 красных намеренно**:
  - `OwnerLifecycleUiE2ETest` — клик «Add Visit» перехватывает чат (issue #1);
  - `KnownDefectsE2ETest`, 2 теста — 500 вместо 404/400 (issues #4, #5).
  Решение пользователя: красный сигнал оставляем, не маскируем. E2E в CI тоже красный по этой же причине.
- Публикация Allure: **на каждом прогоне** (ночной по расписанию и ручной с `publish_report=true`), в CloudFront. Ссылка на отчёт именно этого прогона — в job summary: `builds/<номер прогона>/index.html`, и ссылка на `latest`. Домен CloudFront: `d2cteh9u7mbf08.cloudfront.net`, хранится в переменной репозитория `CLOUDFRONT_DOMAIN`. Проверено: `builds/5` и `latest` отдают правильный прогон.
- Незакоммиченного в `test-framework` нет. Не в git (не трогаем): `.idea/*.xml` (правит IDE), `.kiro/` (чужое).

## 2. Правила работы (задал пользователь)

- Фазы: ветка → коммит → fast-forward в `main` → push. Коммит и push — после явного разрешения на фазу.
- **Приложение (`app/`) не меняем.** Дефекты приложения — отдельные issue, решает пользователь.
- Тесты не подгоняем под текущее поведение приложения. Хаков, которые скрывают дефекты, не добавляем.
- Внешние действия — только с подтверждением: публикация, создание issue, dispatch workflow, настройки репозитория.
- Коммит-трейлер: брать из системного напоминания текущей сессии (он зависит от модели).
- GenAI не поднимаем (нужен ключ, тестов нет).
- Компонентный слой переписываем с нуля, а не чиним.

## 3. Аудит: что было не так (исходное состояние)

Тесты сгенерированы другой LLM. Основное:
1. Тесты подстраивались под приложение: 7 коммитов «Fix…» в `app/`. На чистом коде E2E проходят, поэтому 6 из 7 откатили (`850f771`), `680e250` оставили (404 вместо пустого 200).
2. Ложный тест `GatewayCircuitBreakerTest` проверял WireMock. Удалён.
3. Маскировка дефектов в Page Objects (noValidate, setForce, скрытие чата JS, catch-ignored). Убрано.
4. Компонентные тесты проверяли DAO, а не сервисы (схема `init.sql` — правленая копия). Пробел покрытия, открыт.
5. Инфраструктура: Thread.sleep, статика БД, очистка только в конце теста, mvn не в PATH, JDK 17 по умолчанию.
6. Даты: сервисы отдают в разных форматах (issue #3), фронтенд на день раньше в западных поясах (issue #2).

## 4. Что сделано по фазам (коммиты в `main`)

- **P0:** `850f771` откат app; `f4bbcdb` удалён ложный тест, убраны хаки; `999396a` CLAUDE.md.
- **Инфраструктура:** `afbb3dc` `.gitattributes`.
- **P1:** `710a48d` надёжность (без sleep, безопасные селекторы, навигация через `$router`, трекер очистки в `@AfterEach`).
- **P2:** `425fab2` `Database` вместо статики, `@Container`, `disabledWithoutDocker`, `LocalDate` через `FlexibleLocalDateDeserializer`; `fdef79d` модуль `ai-triage`, типизированный `PetClinicClient`, `TypedClientE2ETest`.
- **P3:** `45d222f` покрытие: `ValidationApiE2ETest` (15), `OwnersAndPetsApiE2ETest` (5), `VisitsApiE2ETest` (4), `ReferenceDataApiE2ETest` (2), `KnownDefectsE2ETest` (2).
- **P4:** `4f0705c` Maven wrapper 3.9.8 (`mvnw`, `mvnw.cmd`), enforcer требует JDK 21, теги `smoke`, `api`, `hybrid`, `ui`, `negative`, `known-defect`.
- **CI-исправления:** `42278ed` terraform.yml не парсился GitHub (тернарный оператор в `${{ }}`); `8ec7c8c` публикация только вручную (потом отменено); `09cac0f` `tflint --init`; `1e48333` в `.tflint.hcl` убраны несуществующие правила.
- **Публикация на каждом прогоне:** `b8ab2f9` (условие публикации и ссылка на прогон в summary); `d3812cf` ссылка через CloudFront вместо закрытого S3 (`AccessDenied`), домен из переменной `CLOUDFRONT_DOMAIN`.
- **Cache-Control:** `22b2f05` отчёты загружаются в S3 с `Cache-Control: no-cache` (проверено на прогоне #6: заголовок отдаётся для `builds/6` и `latest`). Причина: браузер держал старый отчёт, Ctrl+F5 не помогал.
- **Полный локальный стек:** `docker-compose.local-full.yml` в корне (см. раздел 5).

## 5. Полный локальный стек

Команды из корня репозитория:
```
docker compose -f docker-compose.test.yml down -v
cd app && ./mvnw -B -q clean package -DskipTests spring-boot:repackage -pl spring-petclinic-customers-service,spring-petclinic-api-gateway -am && cd ..
docker compose -f docker-compose.test.yml -f docker-compose.local-full.yml up -d --build
```
Сервисы: MySQL, config, discovery, customers и gateway (собраны из нашего кода), visits, vets (образы), admin, tracing (Zipkin), Prometheus, Grafana. GenAI не поднимаем.

Порты: API 8080, customers 8081, visits 8082, vets 8083, Eureka 8761, config 8888, admin 9090, Prometheus 9091, Grafana 3030, Zipkin 9411, MySQL 3306.

Известные особенности:
- `visits-service` может упасть на первом старте (нет таблицы `pets`): `docker compose ... up -d visits-service`.
- Регистрация в Eureka идёт 30–60 с; до этого шлюз отвечает без visits.
- Счётчики БД после тестов: 10 владельцев, 13 питомцев (seed). Иначе остался мусор.

Проверка на полном стеке: E2E 34 теста, 31 зелёный, 3 ожидаемых красных (как на GitHub).

## 6. Дефекты приложения (открытые issue)

- **#1** Плавающий чат перехватывает клик «Add Visit». Чат сворачивается кнопкой «Chat with Us!».
- **#2** Фронтенд: даты на день раньше в западных поясах (`utils/dates.js`). Проверено через `Intl`: Нью-Йорк показывает 6 вместо 7.
- **#3** API: дата питомца приходит как UTC-datetime, дата визита как `yyyy-MM-dd`. Причина: нет `@JsonFormat` у `Pet.birthDate`. **Гипотеза про `TZ` JVM подтверждена (2026-11-16):** локальный JVM в Europe/Kyiv (UTC+3) отдал DATE `2000-09-07` как `2000-09-06T21:00:00.000Z`, то есть на день раньше. В контейнерах стека `TZ=UTC`, поэтому там сдвиг не виден. Комментарий с этим фактом добавлен к issue #3 (2026-11-16).
- **#4** Визит к несуществующему питомцу → 500 (нарушение `visits_ibfk_1` не обрабатывается). Ожидается 404.
- **#5** Питомец с неизвестным `typeId` → 500 (тип не проверяется, NPE в `Pet.toString()`). Ожидается 400.

Не заведено как issue (в бэклоге): пустое описание визита принимается (201), дубликаты телефона принимаются (201).

## 7. Решения, которые ждут пользователя

- Пустое описание визита: `@NotBlank` в visits-service (рекомендация) или оставить.
- Дубликаты телефона у разных владельцев: допустимо или нет.
- Формат дат (issue #3): какой формат считать контрактом API. Тесты пока принимают оба.
- Поиск владельцев в API: нужен ли.
- Переключение публикации с CloudFront на GitHub Pages: позже, возможно.

## 8. Что дальше

2. **Компонентный слой переписать с нуля** на реальных сервисах, по одному сервису, начиная с customers-service. `core` (конфиг, клиент, DAO, Allure) и page objects сохраняем. Старые компонентные тесты не чинить. Осуществимость проверена, подход описан в разделе 10.
3. UI-валидация форм и страница ветеринаров — не покрыты.
4. Решения из раздела 7.
5. GenAI — не поднимать, пока нет теста и ключа.

## 9. Окружение (на этой машине)

- Windows 11, Docker Desktop (контекст `desktop-linux`), `gh` авторизован как `igreukrnet`, AWS CLI с локальным входом (`aws login`, сессия может истечь).
- JDK 21: `C:\Users\root\.jdks\corretto-21.0.13`. По умолчанию JDK 17 (`ms-17.0.16`), enforcer его отсеет.
- Maven не в PATH: `test-framework\mvnw.cmd` (PowerShell) или `./mvnw` (bash).
- `actionlint` для проверки workflow: `$TEMP\al\bin\actionlint.exe` (ставится `python -I -m pip install actionlint-py --target $TEMP\al`, правило разрешено в `.claude/settings.local.json`; временная папка может очиститься — тогда поставить заново).
- Remote: `https://github.com/igreukrnet/petclinic.git` (репозиторий публичный).
- В PowerShell аргументы `-Dgroups=...` с `!` и `=` берём в кавычках.

## 10. Компонентные тесты на реальном сервисе: результат проверки и подход

Проверка (2026-11-16), в репозиторий ничего не вносилось, временные процессы и контейнер удалены.

**Автономный запуск customers-service работает.** Тот же JAR, что в E2E (`app/spring-petclinic-customers-service/target/spring-petclinic-customers-service-4.0.1.jar`), JDK 17, реальная MySQL 8.4.5. Без config-server и Eureka. Нужны параметры:
```
--spring.profiles.active=mysql
--spring.datasource.url=jdbc:mysql://<host>:<port>/petclinic?allowPublicKeyRetrieval=true&useSSL=false
--spring.datasource.username=root --spring.datasource.password=petclinic
--spring.cloud.config.enabled=false --eureka.client.enabled=false
--spring.sql.init.mode=always
--spring.sql.init.schema-locations=classpath:db/mysql/schema.sql
--spring.sql.init.data-locations=classpath:db/mysql/data.sql
```
Без двух последних параметров сервис стартует, но отвечает 500 (`Table 'petclinic.owners' doesn't exist`): в боевом стеке схему настраивает config-server. Старт 15–20 с.

Поведение автономного сервиса: `/owners` отдаёт 10 владельцев, `/petTypes` 6 типов, `POST /owners` с `{}` → 400, `GET /owners/99999` → 404, питомец с `typeId=99` → 500 (issue #5).

**Предложенный подход (ждёт «да»):**
- Сервис в контейнере через Testcontainers из образа, собранного из JAR (`app/docker/Dockerfile`), рядом MySQL в общей сети. JAR уже собирает `fast-checks.yml`.
- Тесты ходят по HTTP прямо в сервис (без шлюза и без префикса `/api/customer`), состояние в БД сверяют через DAO из `core`.
- Для customers-service WireMock не нужен (он не вызывает другие сервисы). Заглушки понадобятся для шлюза: следующий шаг после customers-service.
- Часовой пояс контейнера задаём явно (`TZ=UTC`). Отдельный тест с тегом `known-defect` фиксирует зависимость даты от часового пояса (issue #3).
- Старые компонентные тесты на DAO удаляем целиком.
- Перед написанием остальных сервисов показать пользователю первый тест и согласовать.

### План реализации первого компонентного теста (согласовано, код ещё не писался)

Подход одобрен пользователем (2026-11-16). Реализацию пользователь отложил на следующий чат. Что выяснено для старта:

- **Файлы модуля сейчас:** `component-tests/src/test/java/.../component/BaseComponentTest.java`, `customers/CustomersServiceComponentTest.java`, `visits/VisitsServiceComponentTest.java`, `src/test/resources/db/init.sql`. Все четыре удалить, это старый слой на DAO. Пакет остаётся `org.springframework.samples.petclinic.test.component`.
- **Образ сервиса.** `app/docker/Dockerfile` собирает слоёный образ (`eclipse-temurin:17`, `JarLauncher`), для теста он избыточен. Проще собрать образ из fat JAR через `ImageFromDockerfile`: `FROM eclipse-temurin:17`, копия JAR, `ENTRYPOINT java -jar`. Путь к JAR по умолчанию из каталога модуля: `../../app/spring-petclinic-customers-service/target/spring-petclinic-customers-service-4.0.1.jar`, переопределяется системным свойством. Если JAR нет, тест падает с понятным сообщением «сначала соберите JAR-ы приложения». CI (`fast-checks.yml`, шаг `mvn -B test -pl component-tests`) JAR уже собирает до этого шага.
- **Переменные окружения контейнера сервиса:** `SPRING_PROFILES_ACTIVE=mysql`, `SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/petclinic?allowPublicKeyRetrieval=true&useSSL=false`, `SPRING_DATASOURCE_USERNAME=root`, `SPRING_DATASOURCE_PASSWORD=petclinic`, `SPRING_CLOUD_CONFIG_ENABLED=false`, `EUREKA_CLIENT_ENABLED=false`, `SPRING_SQL_INIT_MODE=always`, `SPRING_SQL_INIT_SCHEMA_LOCATIONS=classpath:db/mysql/schema.sql`, `SPRING_SQL_INIT_DATA_LOCATIONS=classpath:db/mysql/data.sql`, `TZ=UTC`, **`SERVER_PORT=8081`**. Порт нужно задать явно: без config-server порт по умолчанию 8080 (в E2E его даёт config-server).
- **MySQL:** `MySQLContainer` в общей сети с алиасом `mysql`, база `petclinic`, пользователь `root`, пароль `petclinic`. Без `withReuse`, чтобы каждый прогон был чистым.
- **Готовность:** `Wait.forHttp("/petTypes")` со статусом 200 и таймаутом около 3 минут.
- **Клиент и БД:** RestAssured с базовым адресом из `container.getHost()` и `getMappedPort(8081)` (фильтр `AllureRestAssured` есть в зависимостях модуля); БД через `Database.connect(...)` и DAO из `core`.
- **Первые тесты для показа пользователю:** seed (10 владельцев, George Franklin), создание владельца с проверкой строки в БД, невалидные владельцы (400), `id` 0 и -1 (400), несуществующий владелец (404). Затем питомцы, типы, `PUT`. Тесты с тегом `known-defect`: неизвестный `typeId` (400 вместо 500, issue #5), формат даты питомца `yyyy-MM-dd` (issue #3), зависимость от часового пояса (контейнер с `TZ=Europe/Kyiv`, issue #3).
- **Наблюдение:** в `component-tests/` лежит каталог `.allure/` с командной строкой Allure (создан плагином `allure-maven`); не относится к тестам, при необходимости проверить `.gitignore`.

### Статус реализации (2026-11-16)

Сделано: старый компонентный слой удалён, новый `BaseComponentTest` и `CustomersServiceComponentTest` (владельцы: seed, создание, чтение, `PUT`, негативные случаи; 13 запусков, все зелёные, около 2,5 мин). Осталось: питомцы, типы, `PUT` питомца, тесты `known-defect` (issues #5 и #3, зависимость от часового пояса с `TZ=Europe/Kyiv`), затем шлюз с WireMock и остальные сервисы.
