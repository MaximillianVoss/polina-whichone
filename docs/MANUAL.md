# Backend Final — WhichOne: от создания до сдачи

[Условие](ASSIGNMENT.md) · [README](PROJECT-README.md) · [Теория](THEORY.md) · [Задания без ответов](PRACTICE.md) · [Решения](SOLUTIONS.md) · [Проверка](../VERIFICATION.md)

## 1. Требования и результат

По исходному PDF нужно создать новый Spring Boot проект через Spring Initializr, написать достаточное по объёму приложение, структурировать и комментировать код, подключить БД и тесты. Копировать Bookstore и просто переименовать его запрещено.

На оценки 3–4 в PDF добавлены аутентификация, более широкий функционал и GitHub. На 5 отдельно указаны PostgreSQL/MariaDB, самостоятельно изученные возможности Spring Boot, GitHub и интернет-сервер. RESTful service указан как optional. Срок, утверждение темы и конкретную дополнительную технологию в материалах установить нельзя.

В этом решении тема — WhichOne: автор создаёт обучающие или личностные квизы, публикует их, пользователи проходят опубликованные квизы, сервер сохраняет результаты и строит собственную статистику. JWT, OpenAPI и Docker — выбранный стек реализации, а не новые требования преподавателя.

Маршрут работы: новый каркас → POM и настройки → сущности и миграция → DTO → регистрация/JWT → авторские черновики и публикация → серверный расчёт → результаты и статистика → браузерная демонстрация → тесты → подготовка сдачи. Полный код файлов приведён далее, поэтому содержание не ограничивается планом.

## 2. Среда и создание на Mac

Ожидаемый путь проекта ученицы:

~~~text
<локальный-путь>
~~~

Нужен JDK 17 или 21. Maven отдельно не требуется: его скачает Maven Wrapper. В терминале проверь:

~~~bash
java -version
/usr/libexec/java_home -V
~~~

Если JDK21 установлен, но выбран другой Java, установи окружение текущего терминала:

~~~bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
~~~

Если Java отсутствует, сначала установи JDK подходящей архитектуры Mac. Тип процессора этого Mac не установлен; Apple Silicon требует ARM64, Intel — x64. Сам Mac в данной проверке не использовался.

Для создания с нуля открой https://start.spring.io/. Выбери Maven, Java, JAR, Java17, group=com.whichone, artifact=whichone, package=com.whichone. Добавь Web, Validation, Data JPA, Security, OAuth2 Resource Server, H2, PostgreSQL Driver и Flyway Migration. Нажми Generate, распакуй новый проект в указанную папку.

На дату подготовки Initializr предоставляет Boot4; запрос Boot3.5.16 получил HTTP400. Готовый комплект получен из нового каркаса Boot4.0.8 и затем адаптирован на Boot3.5.16/Java17. Это зафиксировано в INITIALIZR.md. Для воспроизведения замени сгенерированный POM полным POM из этого руководства и создай остальные файлы по путям. Классы Bookstore не используются.

Практический быстрый вариант — скопировать готовую папку whichone из комплекта в этот путь. Внутри должны находиться pom.xml, mvnw, mvnw.cmd, .mvn/, src/, scripts/, docs/ и examples/. Открывай именно папку с pom.xml как Maven проект в IDE.

## 3. Индивидуальный запуск и общий комплект

~~~bash
cd <папка-этого-репозитория>
bash ./mvnw -B clean verify
java -jar target/whichone-1.0.0.jar
~~~

Либо одной командой:

~~~bash
bash ./scripts/build-and-run.sh
~~~

Скрипт сначала проверяет проект через clean verify, затем запускает конкретный whichone-1.0.0.jar. Он не выбирает случайный старый JAR по маске. Для другого порта:

~~~bash
PORT=8085 bash ./scripts/build-and-run.sh
~~~

Открой http://localhost:8080/ (либо выбранный порт). Ctrl+C останавливает приложение. Терминал должен оставаться открытым во время демонстрации.

Первый запуск скачивает зависимости и создаёт data/whichone.mv.db. Это файловая H2; отдельно устанавливать MySQL или PostgreSQL для начала не нужно. Рабочая папка определяет место data/. Скрипт всегда переходит в папку проекта. Тесты используют отдельную H2 в памяти и не изменяют учебные аккаунты.

В полном комплекте можно выбрать WhichOne в общем launcher. Общий launcher не обязателен для самостоятельного архива: команды выше работают без соседних проектов. Конкретные команды общего launcher приведены в README/MACOS общего комплекта.

На Windows используется PowerShell:

~~~powershell
.\mvnw.cmd -B clean verify
.\scripts\build-and-run.ps1
~~~

## 4. Демонстрация без отдельного frontend

На главной странице зарегистрируй новое имя: 3–30 латинских букв, цифр или _. Пароль — не меньше10 символов и не больше72 UTF-8 байтов. Нажми «Создать два примера»: сервер создаст и опубликует оба квиза от твоего имени.

В «Основы Java» выбери 4 и table. Первый вопрос даёт3 балла, второй2: результат3/5, или60%. Нажми «Статистика»: одна учебная попытка даёт averagePercent=60 и bestPercent=60.

В «Как ты учишься?» выбери «Составляю план» и «Через эксперименты». Исходы получают по2; PLANNER первым объявлен в outcomes и выигрывает равенство. Личностная попытка увеличит свой счётчик, но не изменит учебный средний процент.

Редактор ниже создаёт собственные черновики из полного JSON. В примерах examples/educational.json и examples/personality.json уже есть рабочие определения. После создания сервер возвращает ID; подставь его в поле ID черновика для замены, публикации или удаления. «Мои квизы» показывает собственные черновики и опубликованные квизы.

PUT заменяет полный черновик, поэтому IDs вопросов/вариантов меняются. Автор должен сохранить исходный JSON для последующего редактирования: API чтения специально не возвращает ключи и веса. После публикации изменение и удаление запрещены; для нового содержания создаётся новый квиз.

Токен хранится только в памяти страницы. Обновление страницы требует нового входа. Выход очищает токен и данные интерфейса; ответы старых запросов от предыдущего аккаунта больше не применяются. Серверный JWT действует до истечения времени: отдельного сервера отзыва/refresh токенов здесь нет.

## 5. Как связаны классы

~~~text
HTTP + Bearer JWT
  → Spring Security: подпись, issuer, audience, срок
  → Controller: URL, входной DTO, @Valid
  → Service: пользователь, владение, правила и расчёт
  → Repository: запросы к БД
  → Entity / Flyway: сохраняемые таблицы
  → безопасный выходной DTO
~~~

| Пакет / файлы | Что делают и почему |
|---|---|
| controller | AuthController регистрирует и выполняет вход; QuizController управляет квизами; MeController возвращает только данные текущего пользователя |
| dto/ApiDtos | Входные и выходные records разделены. Entity не сериализуется наружу; OptionView содержит только id/text |
| domain | AppUser, Quiz, Question, AnswerOption, PersonalityOutcome, QuizAttempt, AttemptAnswer описывают семь таблиц; QuizType выбирает модель расчёта |
| repository | Spring Data JPA выполняет поиск пользователей, опубликованных/собственных квизов и собственных попыток |
| security | BCrypt, выдача JWT, Resource Server decoder и единый формат401/403 |
| service | Проверяет смысл данных, владение, неизменность опубликованных квизов, рассчитывает и сохраняет результат |
| config/OpenApiConfig | Добавляет описание API и BearerAuth в Swagger |
| exception | ApiException и advice возвращают понятные ProblemDetail400/403/404/409 без сырых тел запросов и внутренних SQL сообщений |

Главные методы:

| Метод | Что и зачем |
|---|---|
| AuthService.register | Нормализует имя, проверяет дубликат, кодирует пароль BCrypt, сохраняет обычного пользователя и выдаёт JWT |
| AuthService.login | Сравнивает пароль с хешем. При отсутствии аккаунта также выполняет сравнение с dummy hash; неверные данные дают401 |
| AuthService.requireUser / me | Связывает subject JWT с реальным пользователем; возвращает безопасный профиль |
| TokenService.issue | Подписывает HS256 токен с subject, issuer, audience и временем действия |
| SecurityConfig.jwtSigningKey / jwtDecoder | Берёт ключ из окружения либо создаёт локальный случайный; проверяет подпись, срок, issuer и audience |
| QuizService.validateDefinition | Требует ровно один правильный вариант для учебного вопроса; для личностного проверяет объявленные исходы и отсутствие correct |
| QuizService.fill / create | Создаёт упорядоченные вопросы, варианты и исходы; автор берётся с сервера |
| QuizService.ownedLocked / requireDraft | Проверяет владение и блокирует изменения после публикации; draft update и publish используют одну DB блокировку |
| QuizService.update / delete / publish | Заменяет/удаляет собственный черновик или публикует его; повторная публикация не создаёт новую версию |
| QuizService.get / mine / published / view | Ограничивает видимость черновиков и строит DTO без correct, весов и связи варианта с исходом |
| AttemptService.validatedOptions | Проверяет ровно один вариант на вопрос и ищет optionId только внутри этого questionId |
| AttemptService.submit | В одной транзакции проверяет ответы, считает серверный результат и сохраняет попытку |
| ScoringService.educational | Суммирует points правильных выбранных вариантов и отдельно максимум |
| ScoringService.personality | Складывает веса по исходам; строгое сравнение > оставляет первым объявленный исход при равенстве |
| AttemptService.view / history / get | Возвращает собственные результаты. Для чужого ID результат404 |
| AttemptService.statistics | Считает среднее процентов и максимум учебных попыток; отсутствие учебных данных выражает null |

Конструкторы Entity устанавливают связи и обязательные данные; getters нужны service для чтения. @ManyToOne задаёт принадлежность, @OneToMany cascade сохраняет детей вместе с родителем. Lazy связи читаются внутри транзакций; open-in-view выключен.

Внешний клиент не назначает себе роль или автора. Неизвестные поля role, authorId и score отклоняются. Правильность и веса допустимы в авторском входном QuizInput, но не присутствуют в DTO получения квиза. Проверка ownership выполняется даже при скрытой кнопке.

Статистика берёт среднее процентов с равным весом каждой учебной попытки: для60% и100% среднее80%. Личностные исходы группируются ключом quizId:outcomeCode, чтобы одинаковые коды разных квизов не смешивались.

## 6. OpenAPI и реальные запросы

Открой http://localhost:8080/swagger-ui.html. В POST /api/auth/register нажми Try it out и отправь:

~~~json
{"username":"polina_demo","password":"Study-pass-2026"}
~~~

Это демонстрационные данные, не готовый настоящий аккаунт. Ответ201 содержит accessToken, tokenType=Bearer, expiresIn=3600 и безопасного user. Скопируй accessToken в кнопку Authorize без слова Bearer: Swagger добавит префикс.

POST /api/auth/login принимает те же поля и возвращает200. В POST /api/quizzes вставь полный examples/educational.json; ответ201 содержит ID квиза, вопросов и вариантов. Затем POST /api/quizzes/{id}/publish возвращает200.

Для прохождения отправь только IDs:

~~~json
{"answers":[{"questionId":1,"optionId":1},{"questionId":2,"optionId":4}]}
~~~

Числа здесь иллюстрируют форму, а не заранее назначенные IDs. Возьми фактические IDs из GET своего опубликованного квиза и выбери по одному варианту каждого вопроса. Сервер вернёт201 и сохранённые score/maxScore либо outcomeCode/outcomeLabel; поля score клиента не существует.

Запрос из терминала без отдельного JSON редактора:

~~~bash
TOKEN='PASTE_YOUR_ACCESS_TOKEN'
curl -i -X POST http://localhost:8080/api/quizzes \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  --data-binary @examples/educational.json
~~~

| Endpoint | Доступ / успешный статус |
|---|---|
| POST /api/auth/register | Всем,201 |
| POST /api/auth/login | Всем,200 |
| GET /api/quizzes | Всем: опубликованная лента,200 |
| GET /api/quizzes/{id} | Опубликованный всем; черновик автору,200 |
| GET /api/quizzes/mine | Вошедшему: свои квизы,200 |
| POST /api/quizzes | Вошедшему: создать свой черновик,201 |
| PUT /api/quizzes/{id} | Автору черновика,200 |
| DELETE /api/quizzes/{id} | Автору черновика,204 |
| POST /api/quizzes/{id}/publish | Автору,200 |
| POST /api/quizzes/{id}/attempts | Вошедшему; опубликованный квиз,201 |
| GET /api/me | Безопасный профиль текущего пользователя,200 |
| GET /api/me/attempts | Собственная история,200 |
| GET /api/me/attempts/{id} | Собственная попытка,200 |
| GET /api/me/statistics | Собственная статистика,200 |

Ошибки:400 — неверный JSON, неизвестное поле, валидация или чужой optionId;401 — нет/неверен токен либо пароль;403 — попытка изменить чужой существующий квиз;404 — недоступный черновик/чужая попытка;409 — дубликат имени, собственный неопубликованный квиз при прохождении или изменение опубликованного.

## 7. PostgreSQL и Docker

H2 достаточно для базового локального запуска, но не заменяет отдельный пункт PostgreSQL/MariaDB в шкале оценки5.

Локальный Compose создаёт PostgreSQL16 и приложение. На Mac с уже работающим Docker:

~~~bash
cd <папка-этого-репозитория>
cp .env.example .env
openssl rand -hex 32
~~~

В .env замени DB_PASSWORD новым локальным паролем и JWT_SECRET результатом генерации; DB_USER=whichone. .env.example содержит только placeholders. Не добавляй .env в Git или архив сдачи.

~~~bash
docker compose up --build
~~~

Приложение: http://localhost:8080/. Для остановки: Ctrl+C, затем docker compose down. Именованный том БД сохраняется; удалять его для обычного перезапуска не требуется. app внутри контейнера слушает0.0.0.0, но опубликованные порты Compose привязаны к127.0.0.1.

Если PostgreSQL уже установлен отдельно, создай БД whichone и её пользователя средствами администрирования, затем установи окружение терминала:

~~~bash
export DB_URL='jdbc:postgresql://localhost:5432/whichone'
export DB_USER='whichone'
export DB_PASSWORD='YOUR_LOCAL_DATABASE_PASSWORD'
export JWT_SECRET="$(openssl rand -hex 32)"
java -jar target/whichone-1.0.0.jar --spring.profiles.active=postgres
~~~

В postgres профиле DB_URL, DB_USER, DB_PASSWORD обязательны; JWT_SECRET должен иметь не менее32 UTF-8 байтов. Для стабильности токенов сохраняй один выбранный серверный ключ между запусками. При H2 без ключа он случайно создаётся на каждом старте и прошлые JWT перестают работать.

Flyway применяет V1__initial_schema.sql один раз и записывает версию в flyway_schema_history. Hibernate validate проверяет структуру. После изменения модели добавляют новую миграцию V2, а не меняют уже применённую V1. Для проверки PostgreSQL запусти приложение дважды с одной БД: второй запуск должен показать, что схема актуальна, а регистрация/квизы/попытки сохранились.

В этой среде runtime PostgreSQL/Docker не проверен: Docker daemon отсутствует. Команды и конфигурация подготовлены для повторения на доступной машине.

## 8. Проверка, защита и сдача

~~~bash
bash ./mvnw -B clean verify
node scripts/ui-regression.cjs
~~~

Java проверки:5 pure scoring и22 интеграционных сценария с настоящими регистрацией/входом/JWT, H2 и Flyway. Дополнительная Node проверка воспроизводит смену аккаунтов и запоздалые ответы интерфейса; она не требует npm install.

Фактически проверены Windows/Java21 с release17, отдельный собранный JAR и браузерный путь регистрации → двух квизов → публикации → обоих результатов → статистики. Проверено узкое окно390px. Подробное подтверждение и ограничения находятся в VERIFICATION.md.

Перед сдачей объясни своими словами: зачем DTO, почему score считает сервер, где проверяется автор, как работает tie и почему публикация запрещает редактирование. Выполни задания PRACTICE.md; подробные решения SOLUTIONS.md находятся отдельно.

Для интернет-развёртывания требуется выбранный и разрешённый сервер: PostgreSQL, окружение с настоящими локальными секретами, запуск собранного JAR или контейнера, reverse proxy с HTTPS и внешняя проверка адреса. По умолчанию Java слушает loopback; reverse proxy обращается к локальному порту. Для отдельного контейнерного запуска его внутренний адрес задаётся SERVER_ADDRESS=0.0.0.0. Не открывай учебную H2 или порт PostgreSQL в интернет.

Чек-лист сдачи:

- Новый проект и понятный код; полный README и воспроизводимый запуск.
- Демонстрация регистрации, двух типов квизов, своих результатов и статистики.
- Тесты проходят; чужое изменение запрещено; скрытые ответы не выдаются.
- Собственный содержательный квиз, а не только два примера.
- Согласованная тема, срок и самостоятельно изученная возможность.
- GitHub и проверенный внешний адрес, если этого требует выбранная оценка.
- Реально подтверждённая PostgreSQL/MariaDB для пункта оценки5.

GitHub и внешний сервер этим комплектом не создавались. Нет refresh/revocation токенов, сброса пароля, ограничения частоты входа и пагинации: это учебная граница, описанная явно. Повторные попытки разрешены. Оценку определяет преподаватель.

Официальные технические источники: [JWT в Spring Security6.5](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html), [PasswordEncoder](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/password-encoder.html), [Flyway в Boot3.5](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html), [springdoc для Boot3](https://springdoc.org/v2/). Поведение этого конкретного проекта дополнительно подтверждено его кодом и тестами.

## Полное содержимое файлов проекта

Ниже непосредственно содержимое проверяемых файлов, без многоточий и пропущенных строк. Пути относительны корню WhichOne. Стандартный Maven Wrapper входит в архив; его большой неизменённый служебный код здесь не повторяется. Рабочие БД, зависимости, сборки, исходный каркас Initializr и временные браузерные отчёты не относятся к этим листингам.

### .dockerignore

````text
target
data
.env
.git
.idea
bootstrap
initializr.zip
docs
*.log
````

### .env.example

````dotenv
# Copy to .env locally and replace every placeholder. Never commit .env.
DB_USER=whichone
DB_PASSWORD=replace-with-a-new-local-database-password
JWT_SECRET=replace-with-output-of-openssl-rand-hex-32
PORT=8080
# DB_URL is needed for a host Java run, not for the app service in Compose.
DB_URL=jdbc:postgresql://localhost:5432/whichone
````

### .gitignore

````text
target/
data/
.env
*.log
.DS_Store
.idea/
*.iml
````

### .mvn/wrapper/maven-wrapper.properties

````properties
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
````

### Dockerfile

````dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S whichone && adduser -S whichone -G whichone
WORKDIR /app
RUN mkdir /app/data && chown whichone:whichone /app/data
COPY --from=build /build/target/whichone-1.0.0.jar /app/app.jar
USER whichone
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
````

### compose.yaml

````yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: whichone
      POSTGRES_USER: ${DB_USER:?Set DB_USER in .env}
      POSTGRES_PASSWORD: ${DB_PASSWORD:?Set DB_PASSWORD in .env}
    ports:
      - "127.0.0.1:5432:5432"
    volumes:
      - whichone-db:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U \"$$POSTGRES_USER\" -d \"$$POSTGRES_DB\""]
      interval: 5s
      timeout: 5s
      retries: 20
  app:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: postgres
      DB_URL: jdbc:postgresql://db:5432/whichone
      DB_USER: ${DB_USER:?Set DB_USER in .env}
      DB_PASSWORD: ${DB_PASSWORD:?Set DB_PASSWORD in .env}
      JWT_SECRET: ${JWT_SECRET:?Set JWT_SECRET in .env}
      SERVER_ADDRESS: 0.0.0.0
      PORT: 8080
    ports:
      - "127.0.0.1:${PORT:-8080}:8080"
    depends_on:
      db:
        condition: service_healthy
volumes:
  whichone-db:
````

### examples/educational.json

````json
{
  "title": "Основы Java",
  "description": "Два вопроса. Максимум — 5 баллов.",
  "type": "EDUCATIONAL",
  "outcomes": [],
  "questions": [
    {"text": "Чему равно 2 + 2?", "points": 3, "options": [
      {"text": "4", "correct": true, "weight": 0},
      {"text": "5", "correct": false, "weight": 0}
    ]},
    {"text": "Какое слово объявляет класс?", "points": 2, "options": [
      {"text": "class", "correct": true, "weight": 0},
      {"text": "table", "correct": false, "weight": 0}
    ]}
  ]
}
````

### examples/personality.json

````json
{
  "title": "Как ты учишься?",
  "description": "При равенстве побеждает первый объявленный исход.",
  "type": "PERSONALITY",
  "outcomes": [
    {"code": "PLANNER", "label": "Планировщик", "description": "Тебе нравится сначала составить план."},
    {"code": "EXPLORER", "label": "Исследователь", "description": "Тебе нравится исследовать через практику."}
  ],
  "questions": [
    {"text": "С чего ты начинаешь новую тему?", "points": 0, "options": [
      {"text": "Составляю план", "correct": false, "outcomeCode": "PLANNER", "weight": 2},
      {"text": "Пробую пример", "correct": false, "outcomeCode": "EXPLORER", "weight": 2}
    ]},
    {"text": "Как тебе удобнее закреплять знания?", "points": 0, "options": [
      {"text": "По расписанию", "correct": false, "outcomeCode": "PLANNER", "weight": 2},
      {"text": "Через эксперименты", "correct": false, "outcomeCode": "EXPLORER", "weight": 2}
    ]}
  ]
}
````

### pom.xml

````xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.16</version>
    <relativePath/>
  </parent>
  <groupId>com.whichone</groupId>
  <artifactId>whichone</artifactId>
  <version>1.0.0</version>
  <name>WhichOne</name>
  <description>Educational and personality quizzes with server-side results</description>
  <properties>
    <java.version>17</java.version>
    <springdoc.version>2.8.13</springdoc.version>
  </properties>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-oauth2-resource-server</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
    <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>${springdoc.version}</version></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
  </dependencies>
  <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>
````

### scripts/build-and-run.ps1

````powershell
param([Parameter(ValueFromRemainingArguments=$true)][string[]]$AppArguments)
$ErrorActionPreference = 'Stop'
$projectDirectory = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $projectDirectory
try {
    & .\mvnw.cmd -B clean verify
    if ($LASTEXITCODE -ne 0) { throw "Maven verification failed with exit code $LASTEXITCODE" }
    $jarPath = Join-Path $projectDirectory 'target\whichone-1.0.0.jar'
    if (!(Test-Path -LiteralPath $jarPath -PathType Leaf)) { throw "Expected JAR not found: $jarPath" }
    $port = if ($env:PORT) { $env:PORT } else { '8080' }
    Write-Host "Starting WhichOne. Open http://localhost:$port/"
    & java -jar $jarPath @AppArguments
    if ($LASTEXITCODE -ne 0) { throw "Application stopped with exit code $LASTEXITCODE" }
}
finally { Pop-Location }
````

### scripts/build-and-run.sh

````bash
#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_dir"
if ! command -v java >/dev/null 2>&1; then
  printf '%s\n' 'Java JDK 17 or 21 is required. See docs/MANUAL.md.' >&2
  exit 1
fi
bash ./mvnw -B clean verify
jar_path="target/whichone-1.0.0.jar"
if [[ ! -f "$jar_path" ]]; then
  printf '%s\n' "Expected executable JAR not found: $jar_path" >&2
  exit 1
fi
printf '%s\n' "Starting $jar_path. Open http://localhost:${PORT:-8080}/"
exec java -jar "$jar_path" "$@"
````

### scripts/build-manual.mjs

````javascript
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const project = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const packet = path.resolve(project, '../..');
const docs = path.join(project, 'docs');
const check = process.argv.includes('--check');
const intro = fs.readFileSync(path.join(docs, 'MANUAL-INTRO.md'), 'utf8').trimEnd();
const files = [
  'pom.xml', 'Dockerfile', 'compose.yaml', '.env.example', '.gitignore', '.dockerignore',
  '.mvn/wrapper/maven-wrapper.properties',
].filter(file => fs.existsSync(path.join(project, file)));
function visit(relative) {
  for (const entry of fs.readdirSync(path.join(project, relative), { withFileTypes: true })) {
    const file = relative + '/' + entry.name;
    if (entry.isDirectory()) visit(file);
    else if (entry.isFile()) files.push(file);
    else throw new Error('Unexpected non-file in own source tree: ' + file);
  }
}
for (const directory of ['src', 'examples', 'scripts']) visit(directory);
const ordered = [...new Set(files)].sort();
const language = file => {
  const extension = path.extname(file);
  if (file.endsWith('.env.example')) return 'dotenv';
  if (file === 'Dockerfile') return 'dockerfile';
  return { '.java': 'java', '.xml': 'xml', '.yml': 'yaml', '.yaml': 'yaml',
    '.json': 'json', '.js': 'javascript', '.cjs': 'javascript', '.mjs': 'javascript',
    '.html': 'html', '.css': 'css', '.sql': 'sql', '.sh': 'bash', '.ps1': 'powershell',
    '.properties': 'properties' }[extension] ?? 'text';
};
// Four backticks keep code that itself contains Markdown fences intact.
const fence = '`'.repeat(4);
const blocks = ordered.map(file => {
  const code = fs.readFileSync(path.join(project, file), 'utf8').trimEnd();
  if (code.split(/\r?\n/).some(line => line.startsWith(fence))) throw new Error('Nested long fence: ' + file);
  return '### ' + file + '\n\n' + fence + language(file) + '\n' + code + '\n' + fence + '\n';
}).join('\n');
const appendix = '\n\n## Полное содержимое файлов проекта\n\n'
  + 'Ниже непосредственно содержимое проверяемых файлов, без многоточий и пропущенных строк. '
  + 'Пути относительны корню WhichOne. Стандартный Maven Wrapper входит в архив; '
  + 'его большой неизменённый служебный код здесь не повторяется. Рабочие БД, зависимости, '
  + 'сборки, исходный каркас Initializr и временные браузерные отчёты не относятся к этим листингам.\n\n'
  + blocks;
function content(navigation) {
  return (intro.includes('{{NAVIGATION}}') ? intro.replaceAll('{{NAVIGATION}}', navigation)
    : navigation + '\n\n' + intro) + appendix;
}
const destinations = [[path.join(docs, 'MANUAL.md'), content(
  '[Условие](ASSIGNMENT.md) · [README](PROJECT-README.md) · [Теория](THEORY.md) · '
  + '[Задания без ответов](PRACTICE.md) · [Решения](SOLUTIONS.md) · [Проверка](../VERIFICATION.md)')]];
if (path.relative(packet, project).replaceAll('\\', '/') === 'solutions/whichone' &&
    fs.existsSync(path.join(packet, 'assignments/final-backend-whichone.md'))) {
  destinations.push(path.join(packet, 'guides/final-backend-whichone.md'), content(
    '[Условие (файл исходной ссылки не включён; см. статус упаковки) · [Проект](../README.md) · '
    + 'Общий запуск Mac (файл исходной ссылки не включён; см. статус упаковки) · [Проверки](../VERIFICATION.md)')]);
}
for (const [file, material] of destinations) {
  if (check) {
    if (!fs.existsSync(file) || fs.readFileSync(file, 'utf8') !== material)
      throw new Error('Manual differs from current source/intro: ' + file);
  } else fs.writeFileSync(file, material, 'utf8');
  const actual = fs.readFileSync(file, 'utf8');
  for (const source of ordered) {
    const code = fs.readFileSync(path.join(project, source), 'utf8').trimEnd();
    if (!actual.includes(fence + language(source) + '\n' + code + '\n' + fence))
      throw new Error('Incomplete source listing: ' + source);
  }
}
console.log(`${check ? 'Verified' : 'Generated'} full manuals: ${ordered.length} own files, ${destinations.length} documents, exact source match.`);
````

### scripts/ui-regression.cjs

````javascript
"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

class Element {
  constructor() { this.value = ""; this._text = ""; this.children = []; this.listeners = {}; this.dataset = {}; }
  set textContent(value) { this._text = value; this.children = []; }
  get textContent() { return this._text + this.children.map(child => child.textContent || "").join(" "); }
  replaceChildren(...children) { this.children = children; this._text = ""; }
  append(...children) { this.children.push(...children); }
  addEventListener(name, callback) { this.listeners[name] = callback; }
  scrollIntoView() {}
}
const flush = () => new Promise(resolve => setImmediate(resolve));
const response = (status, value) => ({ status, ok: status < 400, json: async () => value });
const auth = username => ({ accessToken: "fake-" + username, user: { username } });

async function setup() {
  const elements = new Map();
  const pending = new Map();
  const html = fs.readFileSync(path.join(__dirname, "../src/main/resources/static/index.html"), "utf8");
  for (const match of html.matchAll(/\bid="([^"]+)"/g)) elements.set(match[1], new Element());
  const context = vm.createContext({
    document: {
      getElementById(id) { if (!elements.has(id)) elements.set(id, new Element()); return elements.get(id); },
      createElement() { return new Element(); }
    },
    fetch: (url) => pending.has(url) ? pending.get(url).promise : Promise.resolve(response(200, [])),
    console
  });
  vm.runInContext(fs.readFileSync(path.join(__dirname, "../src/main/resources/static/app.js"), "utf8"), context);
  await flush();
  return {
    context, elements,
    setAccount(name) { context.nextAuth = name ? auth(name) : null; vm.runInContext("setAccount(nextAuth)", context); },
    delay(url) {
      let resolve;
      const promise = new Promise(callback => { resolve = callback; });
      pending.set(url, { promise });
      return value => { pending.delete(url); resolve(value); };
    }
  };
}

function putPrivateUi(test) {
  vm.runInContext('currentQuiz = {id: 99, title: "ALICE_PRIVATE"}', test.context);
  for (const id of ["feed", "questions", "quiz-title", "quiz-description", "result", "personal-output", "status"]) {
    test.elements.get(id).textContent = "ALICE_PRIVATE";
  }
  test.elements.get("definition").value = '{"title":"ALICE_PRIVATE"}';
  test.elements.get("draft-id").value = "99";
  test.elements.get("play-panel").hidden = false;
}

function assertPrivateCleared(test) {
  assert.equal(vm.runInContext("currentQuiz", test.context), null);
  assert.equal(test.elements.get("play-panel").hidden, true);
  assert.equal(test.elements.get("draft-id").value, "");
  assert.equal(test.elements.get("submit-attempt").disabled, true);
  for (const id of ["feed", "questions", "quiz-title", "quiz-description", "result", "personal-output", "status"]) {
    assert.ok(!test.elements.get(id).textContent.includes("ALICE_PRIVATE"), "Private DOM retained in " + id);
  }
  assert.ok(!test.elements.get("definition").value.includes("ALICE_PRIVATE"), "Private definition retained");
}

async function run() {
  let passed = 0;
  for (const next of [null, "bob"]) {
    const test = await setup();
    test.setAccount("alice");
    await flush();
    putPrivateUi(test);
    test.setAccount(next);
    await flush();
    assertPrivateCleared(test);
    passed++;
  }

  const mine = await setup();
  mine.setAccount("alice");
  await flush();
  const releaseMine = mine.delay("/api/quizzes/mine");
  const mineRequest = mine.elements.get("mine").listeners.click();
  mine.setAccount("bob");
  releaseMine(response(200, [{ id: 99, title: "ALICE_PRIVATE", description: "", type: "EDUCATIONAL",
    questionCount: 1, author: "alice", published: false }]));
  await mineRequest;
  await flush();
  assert.ok(!mine.elements.get("feed").textContent.includes("ALICE_PRIVATE"), "A stale private feed was rendered");
  assert.ok(mine.elements.get("account").textContent.includes("bob"));
  passed++;

  const denied = await setup();
  denied.setAccount("alice");
  await flush();
  const releaseDenied = denied.delay("/api/me/statistics");
  const deniedRequest = denied.elements.get("statistics").listeners.click();
  denied.setAccount("bob");
  releaseDenied(response(401, { detail: "Expired Alice token" }));
  await deniedRequest;
  await flush();
  assert.ok(denied.elements.get("account").textContent.includes("bob"), "Stale 401 logged Bob out");
  assert.equal(vm.runInContext("token", denied.context), "fake-bob");
  passed++;

  const quiz = await setup();
  quiz.setAccount("alice");
  await flush();
  const releaseQuiz = quiz.delay("/api/quizzes/99");
  const quizRequest = vm.runInContext('action(() => openQuiz(99))', quiz.context);
  quiz.setAccount("bob");
  releaseQuiz(response(200, { id: 99, title: "ALICE_PRIVATE", description: "", published: false, questions: [] }));
  await quizRequest;
  await flush();
  assertPrivateCleared(quiz);
  passed++;
  console.log("WhichOne UI auth regression: " + passed + "/5 PASS");
}
run().catch(error => { console.error(error.message); process.exitCode = 1; });
````

### src/main/java/com/whichone/WhichOneApplication.java

````java
package com.whichone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Independent final project generated from a fresh Spring Initializr skeleton. */
@SpringBootApplication
public class WhichOneApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhichOneApplication.class, args);
    }
}
````

### src/main/java/com/whichone/config/OpenApiConfig.java

````java
package com.whichone.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.*;

@Configuration
@SecurityScheme(name = "BearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
    @Bean
    OpenAPI whichOneApi() {
        return new OpenAPI().info(new Info().title("WhichOne API").version("1.0.0")
                .description("Create drafts, publish immutable quizzes, submit answers, and view your own statistics. "
                        + "Educational scores and personality outcomes are calculated on the server."));
    }
}
````

### src/main/java/com/whichone/controller/AuthController.java

````java
package com.whichone.controller;

import com.whichone.dto.ApiDtos.*;
import com.whichone.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    @Operation(summary = "Register an ordinary user and issue a token", description = "201 on success, 400 invalid input, 409 duplicate username.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest input) {
        return ResponseEntity.created(URI.create("/api/me")).body(auth.register(input));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with username and password", description = "200 on success, 400 invalid input, 401 incorrect credentials.")
    public AuthResponse login(@Valid @RequestBody AuthRequest input) { return auth.login(input); }
}
````

### src/main/java/com/whichone/controller/MeController.java

````java
package com.whichone.controller;

import com.whichone.dto.ApiDtos.*;
import com.whichone.service.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/me")
@Tag(name = "My account and results")
@SecurityRequirement(name = "BearerAuth")
public class MeController {
    private final AuthService auth;
    private final AttemptService attempts;
    public MeController(AuthService auth, AttemptService attempts) { this.auth = auth; this.attempts = attempts; }

    @GetMapping
    @Operation(summary = "Get your safe account profile")
    public UserView me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return auth.me(principal.getSubject());
    }

    @GetMapping("/attempts")
    @Operation(summary = "Get your attempt history")
    public List<AttemptView> history(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return attempts.history(principal.getSubject());
    }

    @GetMapping("/attempts/{id}")
    @Operation(summary = "Get one of your results", description = "A missing or foreign result returns 404.")
    public AttemptView result(@PathVariable long id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return attempts.get(id, principal.getSubject());
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get statistics calculated from your saved attempts",
            description = "Percentages apply only to educational attempts. Unknown averages are null, not zero.")
    public StatisticsView statistics(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return attempts.statistics(principal.getSubject());
    }
}
````

### src/main/java/com/whichone/controller/QuizController.java

````java
package com.whichone.controller;

import com.whichone.dto.ApiDtos.*;
import com.whichone.service.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quizzes")
public class QuizController {
    private final QuizService quizzes;
    private final AttemptService attempts;
    public QuizController(QuizService quizzes, AttemptService attempts) {
        this.quizzes = quizzes;
        this.attempts = attempts;
    }

    @GetMapping
    @Operation(summary = "Get the published quiz feed")
    public List<QuizSummary> published() { return quizzes.published(); }

    @GetMapping("/mine")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get your own drafts and published quizzes")
    public List<QuizSummary> mine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return quizzes.mine(principal.getSubject());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a quiz without answer keys", description = "Drafts are visible only to their author; otherwise 404.")
    public QuizView get(@PathVariable long id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return quizzes.get(id, principal == null ? null : principal.getSubject());
    }

    @PostMapping
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a draft", description = "201; author is always the authenticated user. Semantic validation returns 400.")
    public ResponseEntity<QuizView> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt principal,
                                          @Valid @RequestBody QuizInput input) {
        var quiz = quizzes.create(principal.getSubject(), input);
        return ResponseEntity.created(URI.create("/api/quizzes/" + quiz.id())).body(quiz);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Replace your draft definition", description = "200; 403 foreign quiz, 409 published quiz. Child IDs change after replacement.")
    public QuizView update(@PathVariable long id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal,
                           @Valid @RequestBody QuizInput input) {
        return quizzes.update(id, principal.getSubject(), input);
    }

    @PostMapping("/{id}/publish")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Publish your quiz permanently", description = "200; repeated publication is idempotent; foreign author gets 403.")
    public QuizView publish(@PathVariable long id, @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        return quizzes.publish(id, principal.getSubject());
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete your draft", description = "204; 403 foreign quiz, 409 published quiz.")
    public ResponseEntity<Void> delete(@PathVariable long id,
                                      @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal) {
        quizzes.delete(id, principal.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/attempts")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Submit one option per question and calculate the result",
            description = "201; 400 invalid/foreign option, 409 own unpublished quiz, 404 unavailable quiz. Client scores are never accepted.")
    public ResponseEntity<AttemptView> submit(@PathVariable long id,
                    @Parameter(hidden = true) @AuthenticationPrincipal Jwt principal,
                    @Valid @RequestBody AttemptInput input) {
        var result = attempts.submit(id, principal.getSubject(), input);
        return ResponseEntity.created(URI.create("/api/me/attempts/" + result.id())).body(result);
    }
}
````

### src/main/java/com/whichone/domain/AnswerOption.java

````java
package com.whichone.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "answer_options")
public class AnswerOption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    @Column(nullable = false, length = 300)
    private String text;
    @Column(name = "option_position", nullable = false)
    private int position;
    @Column(nullable = false)
    private boolean correct;
    @Column(length = 24)
    private String outcomeCode;
    @Column(nullable = false)
    private int weight;

    protected AnswerOption() { }

    public AnswerOption(Question question, String text, int position, boolean correct, String outcomeCode, int weight) {
        this.question = question;
        this.text = text;
        this.position = position;
        this.correct = correct;
        this.outcomeCode = outcomeCode;
        this.weight = weight;
    }

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public String getText() { return text; }
    public int getPosition() { return position; }
    public boolean isCorrect() { return correct; }
    public String getOutcomeCode() { return outcomeCode; }
    public int getWeight() { return weight; }
}
````

### src/main/java/com/whichone/domain/AppUser.java

````java
package com.whichone.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30)
    private String username;
    @Column(nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false)
    private Instant createdAt;

    protected AppUser() { }

    public AppUser(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}
````

### src/main/java/com/whichone/domain/AttemptAnswer.java

````java
package com.whichone.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "attempt_answers")
public class AttemptAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private AnswerOption option;

    protected AttemptAnswer() { }

    public AttemptAnswer(QuizAttempt attempt, AnswerOption option) {
        this.attempt = attempt;
        this.option = option;
    }

    public AnswerOption getOption() { return option; }
}
````

### src/main/java/com/whichone/domain/PersonalityOutcome.java

````java
package com.whichone.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "personality_outcomes")
public class PersonalityOutcome {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    @Column(nullable = false, length = 24)
    private String code;
    @Column(nullable = false, length = 120)
    private String label;
    @Column(nullable = false, length = 500)
    private String description;
    @Column(name = "outcome_position", nullable = false)
    private int position;

    protected PersonalityOutcome() { }

    public PersonalityOutcome(Quiz quiz, String code, String label, String description, int position) {
        this.quiz = quiz;
        this.code = code;
        this.label = label;
        this.description = description;
        this.position = position;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public int getPosition() { return position; }
}
````

### src/main/java/com/whichone/domain/Question.java

````java
package com.whichone.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    @Column(nullable = false, length = 1000)
    private String text;
    @Column(name = "question_position", nullable = false)
    private int position;
    @Column(nullable = false)
    private int points;
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<AnswerOption> options = new ArrayList<>();

    protected Question() { }

    public Question(Quiz quiz, String text, int position, int points) {
        this.quiz = quiz;
        this.text = text;
        this.position = position;
        this.points = points;
    }

    public void addOption(AnswerOption option) { options.add(option); }
    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public String getText() { return text; }
    public int getPosition() { return position; }
    public int getPoints() { return points; }
    public List<AnswerOption> getOptions() { return options; }
}
````

### src/main/java/com/whichone/domain/Quiz.java

````java
package com.whichone.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(nullable = false, length = 1000)
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private QuizType type;
    @Column(nullable = false)
    private boolean published;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant publishedAt;
    @Version
    private long version;
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Question> questions = new ArrayList<>();
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PersonalityOutcome> outcomes = new ArrayList<>();

    protected Quiz() { }

    public Quiz(AppUser author) {
        this.author = author;
        this.createdAt = Instant.now();
    }

    public void define(String title, String description, QuizType type) {
        this.title = title;
        this.description = description;
        this.type = type;
    }

    public void clearDraftContents() {
        questions.clear();
        outcomes.clear();
    }

    public void addQuestion(Question question) { questions.add(question); }
    public void addOutcome(PersonalityOutcome outcome) { outcomes.add(outcome); }

    public void publish() {
        if (!published) {
            published = true;
            publishedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public AppUser getAuthor() { return author; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public QuizType getType() { return type; }
    public boolean isPublished() { return published; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public List<Question> getQuestions() { return questions; }
    public List<PersonalityOutcome> getOutcomes() { return outcomes; }
}
````

### src/main/java/com/whichone/domain/QuizAttempt.java

````java
package com.whichone.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    @Column(nullable = false)
    private Instant submittedAt;
    private Integer score;
    private Integer maxScore;
    @Column(length = 24)
    private String outcomeCode;
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<AttemptAnswer> answers = new ArrayList<>();

    protected QuizAttempt() { }

    public QuizAttempt(AppUser user, Quiz quiz, Integer score, Integer maxScore, String outcomeCode) {
        this.user = user;
        this.quiz = quiz;
        this.score = score;
        this.maxScore = maxScore;
        this.outcomeCode = outcomeCode;
        this.submittedAt = Instant.now();
    }

    public void addAnswer(AnswerOption option) { answers.add(new AttemptAnswer(this, option)); }
    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Integer getScore() { return score; }
    public Integer getMaxScore() { return maxScore; }
    public String getOutcomeCode() { return outcomeCode; }
    public List<AttemptAnswer> getAnswers() { return answers; }
}
````

### src/main/java/com/whichone/domain/QuizType.java

````java
package com.whichone.domain;

/** Two deliberately separate scoring models. */
public enum QuizType {
    EDUCATIONAL, PERSONALITY
}
````

### src/main/java/com/whichone/dto/ApiDtos.java

````java
package com.whichone.dto;

import com.whichone.domain.QuizType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request and response contracts are separate: JPA entities are never HTTP responses. */
public final class ApiDtos {
    private ApiDtos() { }

    public record AuthRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,30}") String username,
            @NotBlank @Size(min = 10, max = 72) String password) { }
    public record UserView(long id, String username, Instant createdAt) { }
    public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserView user) { }

    public record QuizInput(
            @NotBlank @Size(max = 120) String title,
            @NotNull @Size(max = 1000) String description,
            @NotNull QuizType type,
            @NotNull @Size(min = 1, max = 30) List<@NotNull @Valid QuestionInput> questions,
            @NotNull @Size(max = 8) List<@NotNull @Valid OutcomeInput> outcomes) { }
    public record QuestionInput(
            @NotBlank @Size(max = 1000) String text,
            @NotNull @Min(0) @Max(10) Integer points,
            @NotNull @Size(min = 2, max = 8) List<@NotNull @Valid OptionInput> options) { }
    public record OptionInput(
            @NotBlank @Size(max = 300) String text,
            boolean correct,
            @Size(max = 24) String outcomeCode,
            @Min(0) @Max(10) int weight) { }
    public record OutcomeInput(
            @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,23}") String code,
            @NotBlank @Size(max = 120) String label,
            @NotNull @Size(max = 500) String description) { }

    public record QuizSummary(long id, String author, String title, String description, QuizType type,
                              boolean published, int questionCount, Instant createdAt, Instant publishedAt) { }
    public record QuizView(long id, String author, String title, String description, QuizType type,
                           boolean published, Instant createdAt, Instant publishedAt,
                           List<QuestionView> questions, List<OutcomeView> outcomes) { }
    public record QuestionView(long id, String text, int points, List<OptionView> options) { }
    // The correctness flag and personality weights exist only in OptionInput and in storage.
    public record OptionView(long id, String text) { }
    public record OutcomeView(String code, String label, String description) { }

    public record AttemptInput(
            @NotNull @Size(min = 1, max = 30) List<@NotNull @Valid AnswerInput> answers) { }
    public record AnswerInput(@NotNull @Positive Long questionId, @NotNull @Positive Long optionId) { }
    public record AnswerFeedback(long questionId, long optionId, Boolean correct) { }
    public record AttemptView(long id, long quizId, String quizTitle, QuizType type, Instant submittedAt,
                              Integer score, Integer maxScore, String outcomeCode, String outcomeLabel,
                              String outcomeDescription, List<AnswerFeedback> answers) { }
    public record StatisticsView(long totalAttempts, long educationalAttempts, long personalityAttempts,
                                 Double averagePercent, Double bestPercent, Map<String, Long> personalityResults) { }
}
````

### src/main/java/com/whichone/exception/ApiException.java

````java
package com.whichone.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
````

### src/main/java/com/whichone/exception/ApiExceptionHandler.java

````java
package com.whichone.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> apiError(ApiException error) {
        return ResponseEntity.status(error.getStatus())
                .body(ProblemDetail.forStatusAndDetail(error.getStatus(), error.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Data conflicts with an existing record."));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    ResponseEntity<ProblemDetail> concurrentChange() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Concurrent change; reload the quiz."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException error,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
        var fields = new LinkedHashMap<String, String>();
        error.getBindingResult().getFieldErrors().forEach(field ->
                fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        problem.setProperty("errors", fields);
        return handleExceptionInternal(error, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException error,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Never reflect raw request bodies or parser details, especially passwords.
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid JSON, unknown field, or incorrect value type.");
        return handleExceptionInternal(error, problem, headers, HttpStatus.BAD_REQUEST, request);
    }
}
````

### src/main/java/com/whichone/repository/AttemptRepository.java

````java
package com.whichone.repository;

import com.whichone.domain.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUserUsernameOrderBySubmittedAtDesc(String username);
    Optional<QuizAttempt> findByIdAndUserUsername(long id, String username);
}
````

### src/main/java/com/whichone/repository/QuizRepository.java

````java
package com.whichone.repository;

import com.whichone.domain.Quiz;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByPublishedTrueOrderByPublishedAtDesc();
    List<Quiz> findByAuthorUsernameOrderByCreatedAtDesc(String username);

    /** Draft changes and publication take the same lock to prevent race-based edits. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quiz q where q.id = :id")
    Optional<Quiz> findLockedById(@Param("id") long id);
}
````

### src/main/java/com/whichone/repository/UserRepository.java

````java
package com.whichone.repository;

import com.whichone.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
````

### src/main/java/com/whichone/security/SecurityConfig.java

````java
package com.whichone.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); }

    @Bean
    SecretKey jwtSigningKey(@Value("${whichone.jwt.secret}") String configured,
                            @Value("${whichone.jwt.require-secret}") boolean required) {
        byte[] bytes;
        if (configured.isBlank()) {
            if (required) {
                throw new IllegalStateException("JWT_SECRET is required for the postgres profile.");
            }
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            LoggerFactory.getLogger(SecurityConfig.class).warn(
                    "Local JWT key generated; local tokens expire on restart. Set JWT_SECRET for persistent tokens.");
        } else {
            bytes = configured.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, @Value("${whichone.jwt.issuer}") String issuer,
                          @Value("${whichone.jwt.audience}") String audience) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
        return decoder;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityProblemHandler problems) throws Exception {
        return http
                // API tokens are in Authorization headers, never in automatically sent cookies.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/app.js", "/styles.css", "/favicon.ico",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/quizzes/mine").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/quizzes", "/api/quizzes/*").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problems).accessDeniedHandler(problems))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults()).authenticationEntryPoint(problems).accessDeniedHandler(problems))
                .build();
    }
}
````

### src/main/java/com/whichone/security/SecurityProblemHandler.java

````java
package com.whichone.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper json;
    public SecurityProblemHandler(ObjectMapper json) { this.json = json; }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException error)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, "A valid Bearer token is required.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException error)
            throws IOException {
        write(response, HttpStatus.FORBIDDEN, "Access denied.");
    }

    private void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detail));
    }
}
````

### src/main/java/com/whichone/security/TokenService.java

````java
package com.whichone.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final long ttlSeconds;

    public TokenService(JwtEncoder encoder, @Value("${whichone.jwt.issuer}") String issuer,
                        @Value("${whichone.jwt.audience}") String audience,
                        @Value("${whichone.jwt.ttl-seconds}") long ttlSeconds) {
        if (ttlSeconds < 60 || ttlSeconds > 86400) {
            throw new IllegalArgumentException("JWT lifetime must be between 60 and 86400 seconds.");
        }
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(String username) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(username).audience(List.of(audience))
                .issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(ttlSeconds)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public long getTtlSeconds() { return ttlSeconds; }
}
````

### src/main/java/com/whichone/service/AttemptService.java

````java
package com.whichone.service;

import com.whichone.domain.*;
import com.whichone.dto.ApiDtos.*;
import com.whichone.exception.ApiException;
import com.whichone.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttemptService {
    private final AttemptRepository attempts;
    private final QuizRepository quizzes;
    private final AuthService auth;
    private final ScoringService scoring;

    public AttemptService(AttemptRepository attempts, QuizRepository quizzes, AuthService auth, ScoringService scoring) {
        this.attempts = attempts;
        this.quizzes = quizzes;
        this.auth = auth;
        this.scoring = scoring;
    }

    public AttemptView submit(long quizId, String username, AttemptInput input) {
        var user = auth.requireUser(username);
        var quiz = quizzes.findById(quizId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found."));
        if (!quiz.isPublished()) {
            if (!quiz.getAuthor().getUsername().equals(username)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Quiz not found.");
            }
            throw new ApiException(HttpStatus.CONFLICT, "Publish the quiz before submitting answers.");
        }
        var selected = validatedOptions(quiz, input);
        Integer score = null;
        Integer maxScore = null;
        String outcomeCode = null;
        if (quiz.getType() == QuizType.EDUCATIONAL) {
            var result = scoring.educational(selected.stream().map(option ->
                    new ScoringService.EducationalAnswer(option.isCorrect(), option.getQuestion().getPoints())).toList());
            score = result.score();
            maxScore = result.maxScore();
        } else {
            outcomeCode = scoring.personality(quiz.getOutcomes().stream().map(PersonalityOutcome::getCode).toList(),
                    selected.stream().map(option ->
                            new ScoringService.PersonalityVote(option.getOutcomeCode(), option.getWeight())).toList());
        }
        var attempt = new QuizAttempt(user, quiz, score, maxScore, outcomeCode);
        selected.forEach(attempt::addAnswer);
        // Validation, calculation and persistence are one transaction: an invalid answer saves nothing.
        return view(attempts.saveAndFlush(attempt));
    }

    @Transactional(readOnly = true)
    public List<AttemptView> history(String username) {
        auth.requireUser(username);
        return attempts.findByUserUsernameOrderBySubmittedAtDesc(username).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public AttemptView get(long id, String username) {
        auth.requireUser(username);
        return view(attempts.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found.")));
    }

    @Transactional(readOnly = true)
    public StatisticsView statistics(String username) {
        auth.requireUser(username);
        var all = attempts.findByUserUsernameOrderBySubmittedAtDesc(username);
        var educational = all.stream().filter(attempt -> attempt.getMaxScore() != null).toList();
        Double average = null;
        Double best = null;
        if (!educational.isEmpty()) {
            average = round(educational.stream().mapToDouble(this::percentage).average().orElseThrow());
            best = round(educational.stream().mapToDouble(this::percentage).max().orElseThrow());
        }
        // Keys include quiz ID so the same outcome code in two quizzes is not conflated.
        var personality = all.stream().filter(attempt -> attempt.getOutcomeCode() != null)
                .collect(Collectors.groupingBy(attempt -> attempt.getQuiz().getId() + ":" + attempt.getOutcomeCode(),
                        TreeMap::new, Collectors.counting()));
        return new StatisticsView(all.size(), educational.size(), all.size() - educational.size(),
                average, best, personality);
    }

    private double percentage(QuizAttempt attempt) { return 100.0 * attempt.getScore() / attempt.getMaxScore(); }
    private double round(double value) { return Math.round(value * 100.0) / 100.0; }

    private List<AnswerOption> validatedOptions(Quiz quiz, AttemptInput input) {
        if (input.answers().size() != quiz.getQuestions().size()) {
            throw invalidAnswers();
        }
        var byQuestion = new HashMap<Long, Long>();
        for (var answer : input.answers()) {
            if (byQuestion.putIfAbsent(answer.questionId(), answer.optionId()) != null) {
                throw invalidAnswers();
            }
        }
        var selected = new ArrayList<AnswerOption>();
        for (var question : quiz.getQuestions()) {
            var optionId = byQuestion.get(question.getId());
            if (optionId == null) { throw invalidAnswers(); }
            // Look up only inside this question: IDs from another question or quiz cannot pass.
            selected.add(question.getOptions().stream().filter(option -> option.getId().equals(optionId))
                    .findFirst().orElseThrow(this::invalidAnswers));
        }
        return selected;
    }

    private ApiException invalidAnswers() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Submit exactly one valid option for every question in this quiz.");
    }

    private AttemptView view(QuizAttempt attempt) {
        var quiz = attempt.getQuiz();
        var outcome = quiz.getOutcomes().stream()
                .filter(candidate -> candidate.getCode().equals(attempt.getOutcomeCode())).findFirst().orElse(null);
        var feedback = attempt.getAnswers().stream().map(AttemptAnswer::getOption)
                .sorted(Comparator.comparingInt(option -> option.getQuestion().getPosition()))
                .map(option -> new AnswerFeedback(option.getQuestion().getId(), option.getId(),
                        quiz.getType() == QuizType.EDUCATIONAL ? option.isCorrect() : null)).toList();
        return new AttemptView(attempt.getId(), quiz.getId(), quiz.getTitle(), quiz.getType(), attempt.getSubmittedAt(),
                attempt.getScore(), attempt.getMaxScore(), attempt.getOutcomeCode(),
                outcome == null ? null : outcome.getLabel(), outcome == null ? null : outcome.getDescription(), feedback);
    }
}
````

### src/main/java/com/whichone/service/AuthService.java

````java
package com.whichone.service;

import com.whichone.domain.AppUser;
import com.whichone.dto.ApiDtos.*;
import com.whichone.exception.ApiException;
import com.whichone.repository.UserRepository;
import com.whichone.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@Transactional
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwords, TokenService tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.dummyHash = passwords.encode("Dummy-account-password-2026");
    }

    public AuthResponse register(AuthRequest request) {
        checkPasswordBytes(request.password());
        String username = normalize(request.username());
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already registered.");
        }
        // No role or author ID is accepted. Every registration creates an ordinary user.
        var user = users.saveAndFlush(new AppUser(username, passwords.encode(request.password())));
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        checkPasswordBytes(request.password());
        var user = users.findByUsername(normalize(request.username()));
        boolean valid = passwords.matches(request.password(), user.map(AppUser::getPasswordHash).orElse(dummyHash));
        if (user.isEmpty() || !valid) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }
        return response(user.get());
    }

    @Transactional(readOnly = true)
    public AppUser requireUser(String username) {
        if (username == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return users.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account is unavailable."));
    }

    @Transactional(readOnly = true)
    public UserView me(String username) { return view(requireUser(username)); }

    private AuthResponse response(AppUser user) {
        return new AuthResponse(tokens.issue(user.getUsername()), "Bearer", tokens.getTtlSeconds(), view(user));
    }

    private UserView view(AppUser user) {
        return new UserView(user.getId(), user.getUsername(), user.getCreatedAt());
    }

    private String normalize(String username) { return username.trim().toLowerCase(Locale.ROOT); }

    private void checkPasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes for BCrypt.");
        }
    }
}
````

### src/main/java/com/whichone/service/QuizService.java

````java
package com.whichone.service;

import com.whichone.domain.*;
import com.whichone.dto.ApiDtos.*;
import com.whichone.exception.ApiException;
import com.whichone.repository.QuizRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class QuizService {
    private final QuizRepository quizzes;
    private final AuthService auth;
    public QuizService(QuizRepository quizzes, AuthService auth) {
        this.quizzes = quizzes;
        this.auth = auth;
    }

    public QuizView create(String username, QuizInput input) {
        validateDefinition(input);
        var quiz = new Quiz(auth.requireUser(username));
        fill(quiz, input);
        return view(quizzes.saveAndFlush(quiz));
    }

    public QuizView update(long id, String username, QuizInput input) {
        var quiz = ownedLocked(id, username);
        requireDraft(quiz);
        validateDefinition(input);
        quiz.clearDraftContents();
        // Delete old ordered children before inserting new ones with the same positions.
        quizzes.flush();
        fill(quiz, input);
        return view(quizzes.saveAndFlush(quiz));
    }

    public QuizView publish(long id, String username) {
        var quiz = ownedLocked(id, username);
        quiz.publish();
        return view(quizzes.saveAndFlush(quiz));
    }

    public void delete(long id, String username) {
        var quiz = ownedLocked(id, username);
        requireDraft(quiz);
        quizzes.delete(quiz);
        quizzes.flush();
    }

    @Transactional(readOnly = true)
    public List<QuizSummary> published() {
        return quizzes.findByPublishedTrueOrderByPublishedAtDesc().stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public List<QuizSummary> mine(String username) {
        auth.requireUser(username);
        return quizzes.findByAuthorUsernameOrderByCreatedAtDesc(username).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public QuizView get(long id, String username) {
        var quiz = quizzes.findById(id).orElseThrow(() -> notFound());
        if (!quiz.isPublished() && !quiz.getAuthor().getUsername().equals(username)) {
            // A stranger cannot enumerate unpublished quizzes.
            throw notFound();
        }
        return view(quiz);
    }

    private Quiz ownedLocked(long id, String username) {
        auth.requireUser(username);
        var quiz = quizzes.findLockedById(id).orElseThrow(() -> notFound());
        if (!quiz.getAuthor().getUsername().equals(username)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the author may change this quiz.");
        }
        return quiz;
    }

    private void requireDraft(Quiz quiz) {
        if (quiz.isPublished()) {
            throw new ApiException(HttpStatus.CONFLICT, "Published quizzes are immutable; create a new quiz.");
        }
    }

    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "Quiz not found."); }
    private void invalid(String message) { throw new ApiException(HttpStatus.BAD_REQUEST, message); }

    private void validateDefinition(QuizInput input) {
        var codes = new HashSet<String>();
        input.outcomes().forEach(outcome -> {
            if (!codes.add(outcome.code())) { invalid("Outcome codes must be unique."); }
        });
        if (input.type() == QuizType.EDUCATIONAL && !codes.isEmpty()) {
            invalid("Educational quizzes must have an empty outcomes list.");
        }
        if (input.type() == QuizType.PERSONALITY && codes.size() < 2) {
            invalid("Personality quizzes require at least two outcomes.");
        }
        for (var question : input.questions()) {
            var texts = new HashSet<String>();
            for (var option : question.options()) {
                if (!texts.add(option.text().trim().toLowerCase(Locale.ROOT))) {
                    invalid("Options in one question must have distinct text.");
                }
            }
            if (input.type() == QuizType.EDUCATIONAL) {
                if (question.points() < 1) { invalid("Educational questions require 1 to 10 points."); }
                long correctCount = question.options().stream().filter(OptionInput::correct).count();
                if (correctCount != 1) { invalid("Each educational question must have exactly one correct option."); }
                if (question.options().stream().anyMatch(option -> option.outcomeCode() != null || option.weight() != 0)) {
                    invalid("Educational options must not contain personality outcome codes or weights.");
                }
            } else {
                if (question.points() != 0) { invalid("Personality question points must be zero."); }
                if (question.options().stream().anyMatch(option -> option.correct() || !codes.contains(option.outcomeCode()))) {
                    invalid("Personality options need a declared outcome code and must not be marked correct.");
                }
            }
        }
    }

    private void fill(Quiz quiz, QuizInput input) {
        quiz.define(input.title().trim(), input.description().trim(), input.type());
        for (int i = 0; i < input.outcomes().size(); i++) {
            var outcome = input.outcomes().get(i);
            quiz.addOutcome(new PersonalityOutcome(quiz, outcome.code(), outcome.label().trim(),
                    outcome.description().trim(), i));
        }
        for (int i = 0; i < input.questions().size(); i++) {
            var definition = input.questions().get(i);
            var question = new Question(quiz, definition.text().trim(), i, definition.points());
            for (int j = 0; j < definition.options().size(); j++) {
                var option = definition.options().get(j);
                question.addOption(new AnswerOption(question, option.text().trim(), j, option.correct(),
                        option.outcomeCode(), option.weight()));
            }
            quiz.addQuestion(question);
        }
    }

    private QuizSummary summary(Quiz quiz) {
        return new QuizSummary(quiz.getId(), quiz.getAuthor().getUsername(), quiz.getTitle(), quiz.getDescription(),
                quiz.getType(), quiz.isPublished(), quiz.getQuestions().size(), quiz.getCreatedAt(), quiz.getPublishedAt());
    }

    private QuizView view(Quiz quiz) {
        var questions = quiz.getQuestions().stream().map(question ->
                new QuestionView(question.getId(), question.getText(), question.getPoints(),
                        question.getOptions().stream().map(option -> new OptionView(option.getId(), option.getText())).toList()))
                .toList();
        var outcomes = quiz.getOutcomes().stream().map(outcome ->
                new OutcomeView(outcome.getCode(), outcome.getLabel(), outcome.getDescription())).toList();
        return new QuizView(quiz.getId(), quiz.getAuthor().getUsername(), quiz.getTitle(), quiz.getDescription(),
                quiz.getType(), quiz.isPublished(), quiz.getCreatedAt(), quiz.getPublishedAt(), questions, outcomes);
    }
}
````

### src/main/java/com/whichone/service/ScoringService.java

````java
package com.whichone.service;

import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;

/** Pure calculations: no HTTP, no database, no client-supplied score. */
@Service
public class ScoringService {
    public record EducationalAnswer(boolean correct, int points) { }
    public record EducationalScore(int score, int maxScore) { }
    public record PersonalityVote(String outcomeCode, int weight) { }

    public EducationalScore educational(List<EducationalAnswer> answers) {
        int max = answers.stream().mapToInt(EducationalAnswer::points).sum();
        int score = answers.stream().filter(EducationalAnswer::correct).mapToInt(EducationalAnswer::points).sum();
        return new EducationalScore(score, max);
    }

    public String personality(List<String> orderedOutcomes, List<PersonalityVote> votes) {
        if (orderedOutcomes.isEmpty()) {
            throw new IllegalArgumentException("No personality outcomes");
        }
        var totals = new LinkedHashMap<String, Integer>();
        orderedOutcomes.forEach(code -> totals.put(code, 0));
        for (var vote : votes) {
            if (!totals.containsKey(vote.outcomeCode()) || vote.weight() < 0) {
                throw new IllegalArgumentException("Invalid personality vote");
            }
            totals.compute(vote.outcomeCode(), (code, total) -> total + vote.weight());
        }
        // Strict > preserves the first declared outcome when totals are equal.
        String winner = orderedOutcomes.get(0);
        for (String code : orderedOutcomes) {
            if (totals.get(code) > totals.get(winner)) {
                winner = code;
            }
        }
        return winner;
    }
}
````

### src/main/resources/application-postgres.yml

````yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
whichone:
  jwt:
    require-secret: true
````

### src/main/resources/application.yml

````yaml
spring:
  application:
    name: whichone
  datasource:
    # Local default requires no database installation; the file survives restarts.
    url: jdbc:h2:file:./data/whichone;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: ""
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
  jackson:
    deserialization:
      fail-on-unknown-properties: true
server:
  address: ${SERVER_ADDRESS:127.0.0.1}
  port: ${PORT:8080}
  error:
    include-message: never
    include-stacktrace: never
whichone:
  jwt:
    secret: ${JWT_SECRET:}
    require-secret: false
    issuer: whichone
    audience: whichone-api
    ttl-seconds: 3600
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    persist-authorization: false
    operations-sorter: alpha
````

### src/main/resources/db/migration/V1__initial_schema.sql

````sql
-- One portable migration for H2 and PostgreSQL. Flyway applies each version once.
CREATE TABLE app_users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE quizzes (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    author_id BIGINT NOT NULL REFERENCES app_users(id),
    title VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('EDUCATIONAL', 'PERSONALITY')),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX quizzes_feed_idx ON quizzes(published, published_at);
CREATE INDEX quizzes_author_idx ON quizzes(author_id);
CREATE TABLE quiz_questions (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    quiz_id BIGINT NOT NULL REFERENCES quizzes(id),
    text VARCHAR(1000) NOT NULL,
    question_position INTEGER NOT NULL,
    points INTEGER NOT NULL CHECK (points BETWEEN 0 AND 10),
    UNIQUE (quiz_id, question_position)
);
CREATE TABLE answer_options (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id),
    text VARCHAR(300) NOT NULL,
    option_position INTEGER NOT NULL,
    correct BOOLEAN NOT NULL DEFAULT FALSE,
    outcome_code VARCHAR(24),
    weight INTEGER NOT NULL CHECK (weight BETWEEN 0 AND 10),
    UNIQUE (question_id, option_position)
);
CREATE TABLE personality_outcomes (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    quiz_id BIGINT NOT NULL REFERENCES quizzes(id),
    code VARCHAR(24) NOT NULL,
    label VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    outcome_position INTEGER NOT NULL,
    UNIQUE (quiz_id, code),
    UNIQUE (quiz_id, outcome_position)
);
CREATE TABLE quiz_attempts (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id),
    quiz_id BIGINT NOT NULL REFERENCES quizzes(id),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    score INTEGER,
    max_score INTEGER,
    outcome_code VARCHAR(24),
    CHECK (score IS NULL OR (score >= 0 AND max_score > 0 AND score <= max_score))
);
CREATE INDEX attempts_user_idx ON quiz_attempts(user_id, submitted_at);
CREATE TABLE attempt_answers (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES quiz_attempts(id),
    option_id BIGINT NOT NULL REFERENCES answer_options(id),
    UNIQUE (attempt_id, option_id)
);
````

### src/main/resources/static/app.js

````javascript
"use strict";

// The demo keeps a token in memory only. No passwords or tokens go to localStorage.
let token = "";
let currentQuiz = null;
let authEpoch = 0;
class StaleResponseError extends Error { }
function checkEpoch(epoch) {
  if (epoch !== authEpoch) throw new StaleResponseError();
}
const byId = id => document.getElementById(id);
const protectedButtons = ["seed", "statistics", "history", "mine", "create-draft",
  "update-draft", "publish-draft", "delete-draft", "submit-attempt"];

function showStatus(message, error = false) {
  byId("status").textContent = message;
  byId("status").dataset.error = String(error);
}

function setAccount(auth = null) {
  authEpoch++;
  token = auth ? auth.accessToken : "";
  currentQuiz = null;
  byId("feed").replaceChildren();
  byId("questions").replaceChildren();
  byId("play-panel").hidden = true;
  byId("quiz-title").textContent = "";
  byId("quiz-description").textContent = "";
  byId("result").textContent = "";
  byId("draft-id").value = "";
  fillEditor(educationalTemplate());
  byId("personal-output").textContent = "Запроси результаты текущего аккаунта.";
  showStatus(auth ? "Аккаунт переключён. Загружается общая лента." : "Сеанс страницы завершён.");
  byId("password").value = "";
  byId("username").value = auth ? auth.user.username : "";
  byId("account").textContent = auth ? "Выполнен вход: " + auth.user.username : "Вход ещё не выполнен.";
  byId("logout").hidden = !auth;
  protectedButtons.forEach(id => { byId(id).disabled = !auth; });
  byId("submit-attempt").disabled = !auth || !currentQuiz || !currentQuiz.published;
  if (!auth) {
    byId("password").value = "";
    byId("personal-output").textContent = "Войди, чтобы увидеть свои результаты.";
    byId("result").textContent = "";
  }
  // Clear private DOM immediately, then fetch only the current account's public feed.
  action(refreshFeed);
}

async function api(path, method = "GET", body = undefined, epoch = authEpoch) {
  checkEpoch(epoch);
  const headers = {};
  if (token) headers.Authorization = "Bearer " + token;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const data = response.status === 204 ? null : await response.json();
  // A late Alice response must not render data or log Bob out after an account switch.
  checkEpoch(epoch);
  if (!response.ok) {
    if (response.status === 401 && token) {
      setAccount(null);
      showStatus("Сессия завершилась. Войди снова.", true);
    }
    const fields = data.errors ? " " + JSON.stringify(data.errors) : "";
    throw new Error("HTTP " + response.status + ": " + (data.detail || "Запрос отклонён.") + fields);
  }
  return data;
}

async function action(work) {
  const epoch = authEpoch;
  try { await work(epoch); } catch (error) {
    if (!(error instanceof StaleResponseError) && epoch === authEpoch) showStatus(error.message, true);
  }
}

const educationalTemplate = () => ({
  title: "Основы Java",
  description: "Два вопроса. Максимум — 5 баллов.",
  type: "EDUCATIONAL", outcomes: [],
  questions: [
    { text: "Чему равно 2 + 2?", points: 3, options: [
      { text: "4", correct: true, weight: 0 }, { text: "5", correct: false, weight: 0 }
    ] },
    { text: "Какое слово объявляет класс?", points: 2, options: [
      { text: "class", correct: true, weight: 0 }, { text: "table", correct: false, weight: 0 }
    ] }
  ]
});

const personalityTemplate = () => ({
  title: "Как ты учишься?",
  description: "Учебный пример с двумя исходами. При равенстве побеждает первый объявленный исход.",
  type: "PERSONALITY",
  outcomes: [
    { code: "PLANNER", label: "Планировщик", description: "Тебе нравится сначала составить план." },
    { code: "EXPLORER", label: "Исследователь", description: "Тебе нравится исследовать через практику." }
  ],
  questions: [
    { text: "С чего ты начинаешь новую тему?", points: 0, options: [
      { text: "Составляю план", correct: false, outcomeCode: "PLANNER", weight: 2 },
      { text: "Пробую пример", correct: false, outcomeCode: "EXPLORER", weight: 2 }
    ] },
    { text: "Как тебе удобнее закреплять знания?", points: 0, options: [
      { text: "По расписанию", correct: false, outcomeCode: "PLANNER", weight: 2 },
      { text: "Через эксперименты", correct: false, outcomeCode: "EXPLORER", weight: 2 }
    ] }
  ]
});

function fillEditor(definition) { byId("definition").value = JSON.stringify(definition, null, 2); }

function element(tag, text, className) {
  const item = document.createElement(tag);
  if (text !== undefined) item.textContent = text;
  if (className) item.className = className;
  return item;
}

function quizCards(quizzes, target, isMine = false) {
  target.replaceChildren();
  if (!quizzes.length) { target.append(element("p", "Пока нет квизов. Создай свой или два готовых примера.")); }
  for (const quiz of quizzes) {
    const card = element("article", undefined, "quiz-card");
    card.append(element("p", quiz.type === "EDUCATIONAL" ? "Обучающий" : "Личностный", "tag"));
    card.append(element("h3", quiz.title));
    card.append(element("p", quiz.description));
    card.append(element("p", quiz.questionCount + " вопроса · автор " + quiz.author));
    const open = element("button", quiz.published ? "Открыть квиз" : "Посмотреть черновик");
    open.addEventListener("click", () => action(async epoch => {
      await openQuiz(quiz.id, epoch);
      checkEpoch(epoch);
      if (isMine && !quiz.published) {
        byId("draft-id").value = quiz.id;
        showStatus("Выбран черновик #" + quiz.id + ". Для изменения введи полный JSON определения.");
      }
    }));
    card.append(open);
    if (isMine) card.append(element("p", "ID " + quiz.id + " · " + (quiz.published ? "опубликован" : "черновик")));
    target.append(card);
  }
}

async function refreshFeed(epoch = authEpoch) {
  const quizzes = await api("/api/quizzes", "GET", undefined, epoch);
  checkEpoch(epoch);
  quizCards(quizzes, byId("feed"));
}

async function openQuiz(id, epoch = authEpoch) {
  const quiz = await api("/api/quizzes/" + id, "GET", undefined, epoch);
  checkEpoch(epoch);
  currentQuiz = quiz;
  byId("quiz-title").textContent = currentQuiz.title;
  byId("quiz-description").textContent = currentQuiz.description;
  byId("questions").replaceChildren();
  byId("result").textContent = "";
  for (const [index, question] of currentQuiz.questions.entries()) {
    const fieldset = element("fieldset");
    fieldset.append(element("legend", (index + 1) + ". " + question.text));
    for (const option of question.options) {
      const label = element("label", undefined, "option");
      const radio = element("input");
      radio.type = "radio";
      radio.name = "question-" + question.id;
      radio.value = String(option.id);
      radio.required = true;
      label.append(radio, element("span", option.text));
      fieldset.append(label);
    }
    byId("questions").append(fieldset);
  }
  byId("play-panel").hidden = false;
  byId("submit-attempt").disabled = !token || !currentQuiz.published;
  byId("play-panel").scrollIntoView({ behavior: "smooth", block: "start" });
}

async function authenticate(path, epoch = authEpoch) {
  const auth = await api(path, "POST", {
    username: byId("username").value, password: byId("password").value
  }, epoch);
  checkEpoch(epoch);
  setAccount(auth);
  byId("password").value = "";
  showStatus("Готово. Ты вошёл как " + auth.user.username + ".");
}

byId("auth-form").addEventListener("submit", event => {
  event.preventDefault();
  action(epoch => authenticate("/api/auth/login", epoch));
});
byId("register").addEventListener("click", () => action(epoch => authenticate("/api/auth/register", epoch)));
byId("logout").addEventListener("click", () => {
  setAccount(null);
  showStatus("Токен удалён из памяти страницы. Для нового запроса можно войти снова.");
});
byId("seed").addEventListener("click", () => action(async epoch => {
  byId("seed").disabled = true;
  try {
    for (const definition of [educationalTemplate(), personalityTemplate()]) {
      const quiz = await api("/api/quizzes", "POST", definition, epoch);
      await api("/api/quizzes/" + quiz.id + "/publish", "POST", undefined, epoch);
    }
    await refreshFeed(epoch);
    checkEpoch(epoch);
    showStatus("Два примера опубликованы. Выбери квиз и отправь ответы.");
  } finally { if (epoch === authEpoch) byId("seed").disabled = !token; }
}));
byId("refresh").addEventListener("click", () => action(refreshFeed));
byId("statistics").addEventListener("click", () => action(async epoch => {
  const result = await api("/api/me/statistics", "GET", undefined, epoch);
  checkEpoch(epoch);
  byId("personal-output").textContent = JSON.stringify(result, null, 2);
  showStatus("Получена статистика твоих сохранённых попыток.");
}));
byId("history").addEventListener("click", () => action(async epoch => {
  const result = await api("/api/me/attempts", "GET", undefined, epoch);
  checkEpoch(epoch);
  byId("personal-output").textContent = JSON.stringify(result, null, 2);
  showStatus("Получена история твоих попыток.");
}));
byId("attempt-form").addEventListener("submit", event => {
  event.preventDefault();
  action(async epoch => {
    const quiz = currentQuiz;
    if (!quiz) throw new Error("Сначала открой квиз текущего аккаунта.");
    const answers = quiz.questions.map(question => {
      const selected = document.querySelector('input[name="question-' + question.id + '"]:checked');
      if (!selected) throw new Error("Выбери один ответ на каждый вопрос.");
      return { questionId: question.id, optionId: Number(selected.value) };
    });
    const result = await api("/api/quizzes/" + quiz.id + "/attempts", "POST", { answers }, epoch);
    checkEpoch(epoch);
    byId("result").textContent = result.type === "EDUCATIONAL"
      ? "Твой результат: " + result.score + " из " + result.maxScore + " баллов."
      : result.outcomeLabel + ". " + result.outcomeDescription;
    showStatus("Попытка #" + result.id + " сохранена. Статистика обновится при запросе.");
  });
});
byId("educational-template").addEventListener("click", () => fillEditor(educationalTemplate()));
byId("personality-template").addEventListener("click", () => fillEditor(personalityTemplate()));
byId("mine").addEventListener("click", () => action(async epoch => {
  const quizzes = await api("/api/quizzes/mine", "GET", undefined, epoch);
  checkEpoch(epoch);
  quizCards(quizzes, byId("feed"), true);
  showStatus("Показаны твои черновики и опубликованные квизы.");
}));
byId("create-draft").addEventListener("click", () => action(async epoch => {
  const quiz = await api("/api/quizzes", "POST", JSON.parse(byId("definition").value), epoch);
  checkEpoch(epoch);
  byId("draft-id").value = quiz.id;
  showStatus("Создан черновик #" + quiz.id + ". Его можно изменить, затем опубликовать.");
}));
function draftId() {
  const id = Number(byId("draft-id").value);
  if (!Number.isSafeInteger(id) || id < 1) throw new Error("Укажи положительный ID своего черновика.");
  return id;
}
byId("update-draft").addEventListener("click", () => action(async epoch => {
  await api("/api/quizzes/" + draftId(), "PUT", JSON.parse(byId("definition").value), epoch);
  checkEpoch(epoch);
  showStatus("Черновик заменён. ID вопросов и вариантов обновились.");
}));
byId("publish-draft").addEventListener("click", () => action(async epoch => {
  await api("/api/quizzes/" + draftId() + "/publish", "POST", undefined, epoch);
  await refreshFeed(epoch);
  checkEpoch(epoch);
  showStatus("Квиз опубликован окончательно. Для изменений создай новый квиз.");
}));
byId("delete-draft").addEventListener("click", () => action(async epoch => {
  await api("/api/quizzes/" + draftId(), "DELETE", undefined, epoch);
  checkEpoch(epoch);
  byId("draft-id").value = "";
  await refreshFeed(epoch);
  checkEpoch(epoch);
  showStatus("Твой черновик удалён.");
}));

fillEditor(educationalTemplate());
action(refreshFeed);
````

### src/main/resources/static/index.html

````html
<!doctype html>
<html lang="ru">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>WhichOne — викторины</title>
  <link rel="icon" href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 40 40'%3E%3Crect width='40' height='40' rx='10' fill='%23106650'/%3E%3Ctext x='10' y='29' font-size='29' fill='white'%3E?%3C/text%3E%3C/svg%3E">
  <link rel="stylesheet" href="/styles.css">
  <script src="/app.js" defer></script>
</head>
<body>
<header>
  <a class="brand" href="/">WhichOne<span>Учись. Выбирай. Узнавай результат.</span></a>
  <a href="/swagger-ui.html">Документация API ↗</a>
</header>
<main>
  <section class="hero">
    <p class="eyebrow">Итоговый проект · Spring Boot</p>
    <h1>Твой следующий вопрос<br>начинается здесь.</h1>
    <p>Создавай обучающие и личностные викторины. Проходи опубликованные квизы и наблюдай за своими результатами.</p>
  </section>
  <p id="status" role="status" aria-live="polite">Лента доступна всем. Для создания и прохождения войди в аккаунт.</p>
  <div class="columns">
    <section class="panel">
      <h2>1. Аккаунт</h2>
      <form id="auth-form">
        <label for="username">Имя: 3–30 латинских букв, цифр или _</label>
        <input id="username" name="username" autocomplete="username" minlength="3" maxlength="30" required>
        <label for="password">Пароль: от 10 символов</label>
        <input id="password" name="password" type="password" autocomplete="current-password" minlength="10" maxlength="72" required>
        <div class="actions">
          <button type="submit">Войти</button>
          <button type="button" id="register" class="secondary">Зарегистрироваться</button>
        </div>
      </form>
      <p id="account">Вход ещё не выполнен.</p>
      <button id="logout" class="secondary" hidden>Выйти из демонстрации</button>
      <p class="note">Токен хранится только в памяти этой страницы. После обновления страницы нужно войти снова.</p>
    </section>
    <section class="panel">
      <h2>2. Готовые примеры</h2>
      <p>Создай два своих опубликованных квиза: «Основы Java» и «Как ты учишься?». Затем выбери квиз в ленте.</p>
      <button id="seed" disabled>Создать два примера</button>
      <h3>Твои результаты</h3>
      <div class="actions">
        <button id="statistics" class="secondary" disabled>Статистика</button>
        <button id="history" class="secondary" disabled>История попыток</button>
      </div>
      <pre id="personal-output">Здесь появятся результаты твоих запросов.</pre>
    </section>
  </div>
  <section class="panel">
    <div class="section-heading"><h2>3. Лента викторин</h2><button id="refresh" class="secondary">Обновить</button></div>
    <div id="feed" class="quiz-grid"></div>
  </section>
  <section id="play-panel" class="panel" hidden>
    <h2 id="quiz-title"></h2>
    <p id="quiz-description"></p>
    <form id="attempt-form">
      <div id="questions"></div>
      <button id="submit-attempt" type="submit" disabled>Отправить ответы</button>
    </form>
    <div id="result" role="status" aria-live="polite"></div>
  </section>
  <section class="panel">
    <h2>4. Создай собственный квиз</h2>
    <p>Редактор принимает полный JSON определения. Правильность и веса задаёт автор, а читатель получает только тексты вариантов. После публикации квиз сохраняется окончательно.</p>
    <div class="actions">
      <button id="educational-template" class="secondary">Шаблон обучающего</button>
      <button id="personality-template" class="secondary">Шаблон личностного</button>
      <button id="mine" class="secondary" disabled>Мои квизы</button>
    </div>
    <label for="definition">Определение квиза</label>
    <textarea id="definition" spellcheck="false" rows="15"></textarea>
    <label for="draft-id">ID черновика для изменения, публикации или удаления</label>
    <input id="draft-id" type="number" min="1" inputmode="numeric">
    <div class="actions">
      <button id="create-draft" disabled>Создать черновик</button>
      <button id="update-draft" class="secondary" disabled>Заменить черновик</button>
      <button id="publish-draft" class="secondary" disabled>Опубликовать окончательно</button>
      <button id="delete-draft" class="secondary" disabled>Удалить черновик</button>
    </div>
  </section>
</main>
<footer>WhichOne · Сервер проверяет ответы, права и результаты · <a href="/swagger-ui.html">OpenAPI</a></footer>
</body>
</html>
````

### src/main/resources/static/styles.css

````css
:root {
  color-scheme: light;
  --ink: #152b27; --muted: #4a6560; --accent: #106650;
  --surface: #fff; --page: #f4f6f0; --line: #d5dfd8;
}
* { box-sizing: border-box; }
body { margin: 0; background: var(--page); color: var(--ink); font: 16px/1.6 system-ui, sans-serif; }
header, main, footer { max-width: 1100px; margin: auto; padding: 24px; }
header { display: flex; align-items: center; justify-content: space-between; gap: 20px; border-bottom: 1px solid var(--line); }
a { color: var(--accent); }
.brand { text-decoration: none; font-size: 28px; font-weight: 750; color: var(--ink); }
.brand span { display: block; font-size: 12px; font-weight: 400; color: var(--muted); }
.hero { padding: 36px 0 20px; max-width: 780px; }
.hero h1 { font-size: clamp(30px, 5vw, 52px); line-height: 1.12; letter-spacing: -1.5px; margin: 14px 0 22px; }
.hero p { color: var(--muted); }
.eyebrow { font-size: 12px; letter-spacing: 1.8px; text-transform: uppercase; }
h2 { font-size: 22px; line-height: 1.3; margin: 0 0 16px; }
h3 { font-size: 17px; margin: 22px 0 10px; }
.columns { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.panel { border: 1px solid var(--line); background: var(--surface); padding: 26px; margin: 20px 0; border-radius: 16px; min-width: 0; }
.columns .panel { margin: 0; }
label { display: block; margin: 14px 0 5px; }
input, textarea { max-width: 100%; width: 100%; padding: 11px 12px; border: 1px solid var(--line); border-radius: 8px; background: var(--surface); color: var(--ink); font: inherit; }
textarea { font: 13px/1.5 ui-monospace, SFMono-Regular, Consolas, monospace; resize: vertical; }
input[type="radio"] { width: auto; margin-right: 9px; }
button { border: 1px solid var(--accent); background: var(--accent); color: #fff; padding: 10px 15px; font: inherit; border-radius: 8px; cursor: pointer; }
button.secondary { background: var(--surface); color: var(--accent); }
button:disabled { opacity: .45; cursor: default; }
button:focus-visible, input:focus-visible, textarea:focus-visible, a:focus-visible { outline: 3px solid #d69932; outline-offset: 3px; }
.actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 16px; }
.note, footer { font-size: 13px; color: var(--muted); }
#status { background: #e4ece4; border-left: 4px solid var(--accent); padding: 12px 15px; overflow-wrap: anywhere; }
#status[data-error="true"] { border-left-color: #a23030; background: #fae8e3; color: #812525; }
pre { font-size: 12px; line-height: 1.5; padding: 12px; border: 1px solid var(--line); border-radius: 8px; max-height: 280px; overflow: auto; white-space: pre-wrap; overflow-wrap: anywhere; }
.section-heading { display: flex; justify-content: space-between; align-items: start; gap: 15px; }
.quiz-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(240px, 100%), 1fr)); gap: 15px; }
.quiz-card { border: 1px solid var(--line); border-radius: 10px; padding: 18px; overflow-wrap: anywhere; }
.quiz-card h3 { margin: 0 0 10px; }
.quiz-card p { color: var(--muted); font-size: 14px; }
.quiz-card .tag { font-size: 11px; text-transform: uppercase; letter-spacing: .8px; }
fieldset { border: 1px solid var(--line); border-radius: 10px; padding: 16px; margin: 16px 0; }
legend { font-weight: 650; }
.option { display: flex; align-items: baseline; cursor: pointer; }
#result { padding-top: 16px; font-size: 18px; overflow-wrap: anywhere; }
[hidden] { display: none !important; }
@media (max-width: 660px) {
  header, main, footer { padding: 18px; }
  header { align-items: start; flex-direction: column; gap: 8px; }
  .columns { grid-template-columns: 1fr; }
  .panel { padding: 18px; }
}
````

### src/test/java/com/whichone/WhichOneApiTest.java

````java
package com.whichone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whichone.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WhichOneApiTest {
    private static final AtomicInteger SERIAL = new AtomicInteger();
    private static final String PASSWORD = "Study-pass-2026";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @Autowired Flyway flyway;
    private String username;
    private String token;

    @BeforeEach
    void registerThroughRealHttp() throws Exception {
        username = "learner_" + SERIAL.incrementAndGet();
        token = register(username).path("accessToken").asText();
    }

    @Test
    void registrationStoresBcryptAndReturnsSafeUserAndWorkingToken() throws Exception {
        var stored = users.findByUsername(username).orElseThrow();
        assertThat(stored.getPasswordHash()).startsWith("$2a$");
        assertThat(passwords.matches(PASSWORD, stored.getPasswordHash())).isTrue();
        var me = call(get("/api/me").header("Authorization", bearer(token)), 200);
        assertThat(me.path("username").asText()).isEqualTo(username);
        assertThat(me.toString()).doesNotContain("password", "hash", "role");
    }

    @Test
    void duplicateNamesAreCaseInsensitiveAndReturnConflict() throws Exception {
        call(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username.toUpperCase(), "password", PASSWORD))), 409);
    }

    @Test
    void loginRejectsWrongPasswordAndAuthenticatesCorrectPassword() throws Exception {
        call(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", "Wrong-pass-2026"))), 401);
        var login = call(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", PASSWORD))), 200);
        call(get("/api/me").header("Authorization", bearer(login.path("accessToken").asText())), 200);
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        call(get("/api/me"), 401);
        call(get("/api/quizzes/mine"), 401);
        call(get("/api/me/statistics"), 401);
    }

    @Test
    void draftIsVisibleOnlyToOwnerAndPublishedQuizIsInPublicFeed() throws Exception {
        var quiz = createEducational(token);
        long id = quiz.path("id").asLong();
        call(get("/api/quizzes/" + id), 404);
        call(get("/api/quizzes/" + id).header("Authorization", bearer(token)), 200);
        String foreign = register("foreign_" + SERIAL.incrementAndGet()).path("accessToken").asText();
        call(get("/api/quizzes/" + id).header("Authorization", bearer(foreign)), 404);
        publish(id, token);
        var publicQuiz = call(get("/api/quizzes/" + id), 200);
        assertThat(publicQuiz.path("published").asBoolean()).isTrue();
        var feed = call(get("/api/quizzes"), 200);
        assertThat(feed.toString()).contains(quiz.path("title").asText());
    }

    @Test
    void foreignUserCannotUpdatePublishOrDeleteQuiz() throws Exception {
        var quiz = createEducational(token);
        long id = quiz.path("id").asLong();
        String foreign = register("foreign_" + SERIAL.incrementAndGet()).path("accessToken").asText();
        call(put("/api/quizzes/" + id).header("Authorization", bearer(foreign))
                .contentType(MediaType.APPLICATION_JSON).content(educationalJson()), 403);
        call(post("/api/quizzes/" + id + "/publish").header("Authorization", bearer(foreign)), 403);
        call(delete("/api/quizzes/" + id).header("Authorization", bearer(foreign)), 403);
        assertThat(call(get("/api/quizzes/" + id).header("Authorization", bearer(token)), 200)
                .path("published").asBoolean()).isFalse();
    }

    @Test
    void ownerCanReplaceOrDeleteDraft() throws Exception {
        var quiz = createEducational(token);
        long id = quiz.path("id").asLong();
        var updated = call(put("/api/quizzes/" + id).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(educationalJson()), 200);
        assertThat(updated.path("id").asLong()).isEqualTo(id);
        mvc.perform(delete("/api/quizzes/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        call(get("/api/quizzes/" + id).header("Authorization", bearer(token)), 404);
    }

    @Test
    void publishedQuizCannotBeChangedOrDeletedAndPublishIsIdempotent() throws Exception {
        var quiz = createEducational(token);
        long id = quiz.path("id").asLong();
        publish(id, token);
        publish(id, token);
        call(put("/api/quizzes/" + id).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(educationalJson()), 409);
        call(delete("/api/quizzes/" + id).header("Authorization", bearer(token)), 409);
    }

    @Test
    void unpublishedQuizCannotBeAttemptedEvenByAuthor() throws Exception {
        var quiz = createEducational(token);
        submit(quiz, token, answers(quiz, 0, 0), 409);
    }

    @Test
    void educationalScoreIsComputedFromServerAnswersAndQuestionPoints() throws Exception {
        var quiz = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        var attempt = submit(quiz, token, answers(quiz, 0, 1), 201);
        assertThat(attempt.path("score").asInt()).isEqualTo(3);
        assertThat(attempt.path("maxScore").asInt()).isEqualTo(5);
        assertThat(attempt.path("answers").get(0).path("correct").asBoolean()).isTrue();
        assertThat(attempt.path("answers").get(1).path("correct").asBoolean()).isFalse();
    }

    @Test
    void foreignQuizOptionIsRejectedWithoutSavingAttempt() throws Exception {
        var quiz = createEducational(token);
        var other = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        var invalid = answers(quiz, 0, 0);
        invalid.get(0).put("optionId", other.path("questions").get(0).path("options").get(0).path("id").asLong());
        submit(quiz, token, invalid, 400);
        assertThat(call(get("/api/me/attempts").header("Authorization", bearer(token)), 200).size()).isZero();
    }

    @Test
    void optionBelongingToAnotherQuestionIsRejected() throws Exception {
        var quiz = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        var invalid = answers(quiz, 0, 0);
        invalid.get(0).put("optionId", quiz.path("questions").get(1).path("options").get(0).path("id").asLong());
        submit(quiz, token, invalid, 400);
    }

    @Test
    void missingOrDuplicateAnswersAreRejected() throws Exception {
        var quiz = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        var two = answers(quiz, 0, 0);
        submit(quiz, token, List.of(two.get(0)), 400);
        submit(quiz, token, List.of(two.get(0), two.get(0)), 400);
    }

    @Test
    void personalityTieReturnsFirstDeclaredOutcome() throws Exception {
        var quiz = call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(personalityJson()), 201);
        publish(quiz.path("id").asLong(), token);
        var result = submit(quiz, token, answers(quiz, 0, 1), 201);
        assertThat(result.path("outcomeCode").asText()).isEqualTo("ZEBRA");
        assertThat(result.path("outcomeLabel").asText()).isEqualTo("Планировщик");
        assertThat(result.path("score").isNull()).isTrue();
    }

    @Test
    void statisticsUseSavedAttemptsAndAveragePercentIsNullUntilEducationalAttempt() throws Exception {
        var before = call(get("/api/me/statistics").header("Authorization", bearer(token)), 200);
        assertThat(before.path("totalAttempts").asLong()).isZero();
        assertThat(before.path("averagePercent").isNull()).isTrue();
        var quiz = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        submit(quiz, token, answers(quiz, 0, 1), 201); // 60%
        submit(quiz, token, answers(quiz, 0, 0), 201); // 100%
        var personal = call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(personalityJson()), 201);
        publish(personal.path("id").asLong(), token);
        submit(personal, token, answers(personal, 0, 0), 201);
        var after = call(get("/api/me/statistics").header("Authorization", bearer(token)), 200);
        assertThat(after.path("totalAttempts").asLong()).isEqualTo(3);
        assertThat(after.path("educationalAttempts").asLong()).isEqualTo(2);
        assertThat(after.path("personalityAttempts").asLong()).isEqualTo(1);
        assertThat(after.path("averagePercent").asDouble()).isEqualTo(80.0);
        assertThat(after.path("bestPercent").asDouble()).isEqualTo(100.0);
    }

    @Test
    void attemptHistoryBelongsToAuthenticatedUserOnly() throws Exception {
        var quiz = createEducational(token);
        publish(quiz.path("id").asLong(), token);
        var result = submit(quiz, token, answers(quiz, 0, 0), 201);
        long id = result.path("id").asLong();
        String foreign = register("foreign_" + SERIAL.incrementAndGet()).path("accessToken").asText();
        call(get("/api/me/attempts/" + id).header("Authorization", bearer(foreign)), 404);
        assertThat(call(get("/api/me/attempts").header("Authorization", bearer(foreign)), 200).size()).isZero();
        call(get("/api/me/attempts/" + id).header("Authorization", bearer(token)), 200);
    }

    @Test
    void quizReadDtosNeverLeakCorrectFlagsOutcomeWeightsOrHashes() throws Exception {
        var quiz = createEducational(token);
        assertThat(quiz.toString()).doesNotContain("correct", "weight", "password", "outcomeCode");
        publish(quiz.path("id").asLong(), token);
        assertThat(call(get("/api/quizzes/" + quiz.path("id").asLong()), 200).toString())
                .doesNotContain("correct", "weight", "password", "outcomeCode");
        var personal = call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(personalityJson()), 201);
        publish(personal.path("id").asLong(), token);
        assertThat(call(get("/api/quizzes/" + personal.path("id").asLong()), 200).toString())
                .doesNotContain("correct", "weight", "outcomeCode");
    }

    @Test
    void invalidInputAndUnknownFieldsReturn400() throws Exception {
        call(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ok_name\",\"password\":\"short\"}"), 400);
        call(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin_try\",\"password\":\"Study-pass-2026\",\"role\":\"ADMIN\"}"), 400);
        var quiz = json.readTree(educationalJson());
        ((com.fasterxml.jackson.databind.node.ObjectNode) quiz).put("authorId", 999);
        call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(quiz.toString()), 400);
        var valid = createEducational(token);
        publish(valid.path("id").asLong(), token);
        call(post("/api/quizzes/" + valid.path("id").asLong() + "/attempts")
                .header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[],\"score\":999}"), 400);
    }

    @Test
    void quizSemanticValidationRejectsAmbiguousCorrectAnswersAndUnknownOutcomes() throws Exception {
        String badEducational = educationalJson().replace("\"correct\":true", "\"correct\":false");
        call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(badEducational), 400);
        String badPersonality = personalityJson().replace("\"outcomeCode\":\"ZEBRA\"", "\"outcomeCode\":\"FOREIGN\"");
        call(post("/api/quizzes").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(badPersonality), 400);
    }

    @Test
    void forgedExpiredAndWrongIssuerTokensAreRejected() throws Exception {
        call(get("/api/me").header("Authorization", "Bearer definitely.not.signed"), 401);
        String expired = issueToken(Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600), "whichone", "whichone-api");
        call(get("/api/me").header("Authorization", bearer(expired)), 401);
        String wrongIssuer = issueToken(Instant.now(), Instant.now().plusSeconds(3600), "foreign", "whichone-api");
        call(get("/api/me").header("Authorization", bearer(wrongIssuer)), 401);
        String wrongAudience = issueToken(Instant.now(), Instant.now().plusSeconds(3600), "whichone", "foreign-api");
        call(get("/api/me").header("Authorization", bearer(wrongAudience)), 401);
    }

    @Test
    void flywaySecondMigrationIsNoOpAndRetainsUser() {
        assertThat(flyway.info().applied()).hasSize(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(users.findByUsername(username)).isPresent();
    }

    @Test
    void homepageDemoAndOpenApiAreAccessible() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("WhichOne")));
        mvc.perform(get("/app.js")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        var docs = call(get("/v3/api-docs"), 200);
        assertThat(docs.path("paths").has("/api/auth/login")).isTrue();
        assertThat(docs.path("components").path("securitySchemes").has("BearerAuth")).isTrue();
    }

    private String issueToken(Instant issued, Instant expires, String issuer, String audience) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(username)
                .audience(List.of(audience)).issuedAt(issued).expiresAt(expires).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private JsonNode register(String name) throws Exception {
        return call(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", name, "password", PASSWORD))), 201);
    }

    private JsonNode createEducational(String accessToken) throws Exception {
        return call(post("/api/quizzes").header("Authorization", bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON).content(educationalJson()), 201);
    }

    private void publish(long id, String accessToken) throws Exception {
        call(post("/api/quizzes/" + id + "/publish").header("Authorization", bearer(accessToken)), 200);
    }

    private JsonNode submit(JsonNode quiz, String accessToken, List<Map<String, Long>> answers, int expectedStatus) throws Exception {
        return call(post("/api/quizzes/" + quiz.path("id").asLong() + "/attempts")
                .header("Authorization", bearer(accessToken)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("answers", answers))), expectedStatus);
    }

    private List<Map<String, Long>> answers(JsonNode quiz, int firstOption, int secondOption) {
        var first = quiz.path("questions").get(0);
        var second = quiz.path("questions").get(1);
        return List.of(
                new java.util.HashMap<>(Map.of("questionId", first.path("id").asLong(),
                        "optionId", first.path("options").get(firstOption).path("id").asLong())),
                new java.util.HashMap<>(Map.of("questionId", second.path("id").asLong(),
                        "optionId", second.path("options").get(secondOption).path("id").asLong())));
    }

    private JsonNode call(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String response = mvc.perform(request).andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return response.isBlank() ? json.nullNode() : json.readTree(response);
    }

    private static String bearer(String accessToken) { return "Bearer " + accessToken; }

    private String educationalJson() {
        return """
                {"title":"Java basics %s","description":"Two questions, five points","type":"EDUCATIONAL","outcomes":[],
                 "questions":[
                   {"text":"How much is 2 + 2?","points":3,"options":[
                     {"text":"4","correct":true,"weight":0},{"text":"5","correct":false,"weight":0}]},
                   {"text":"Which keyword creates a class?","points":2,"options":[
                     {"text":"class","correct":true,"weight":0},{"text":"table","correct":false,"weight":0}]}]}
                """.formatted(SERIAL.incrementAndGet());
    }

    private String personalityJson() {
        return """
                {"title":"Learning style","description":"A demonstration, not a medical assessment","type":"PERSONALITY",
                 "outcomes":[
                   {"code":"ZEBRA","label":"Планировщик","description":"You prefer a plan."},
                   {"code":"ALPHA","label":"Исследователь","description":"You prefer experimenting."}],
                 "questions":[
                   {"text":"How do you start?","points":0,"options":[
                     {"text":"Make a plan","correct":false,"outcomeCode":"ZEBRA","weight":2},
                     {"text":"Try something","correct":false,"outcomeCode":"ALPHA","weight":2}]},
                   {"text":"What helps you learn?","points":0,"options":[
                     {"text":"A schedule","correct":false,"outcomeCode":"ZEBRA","weight":2},
                     {"text":"Experiments","correct":false,"outcomeCode":"ALPHA","weight":2}]}]}
                """;
    }
}
````

### src/test/java/com/whichone/service/ScoringServiceTest.java

````java
package com.whichone.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoringServiceTest {
    private final ScoringService scoring = new ScoringService();

    @Test
    void educationalUsesQuestionPointsAndNotNumberOfAnswers() {
        var result = scoring.educational(List.of(
                new ScoringService.EducationalAnswer(true, 3),
                new ScoringService.EducationalAnswer(false, 2),
                new ScoringService.EducationalAnswer(true, 1)));
        assertThat(result.score()).isEqualTo(4);
        assertThat(result.maxScore()).isEqualTo(6);
    }

    @Test
    void personalitySelectsHighestTotal() {
        var result = scoring.personality(List.of("PLANNER", "EXPLORER"), List.of(
                new ScoringService.PersonalityVote("PLANNER", 1),
                new ScoringService.PersonalityVote("EXPLORER", 3)));
        assertThat(result).isEqualTo("EXPLORER");
    }

    @Test
    void personalityTieUsesDeclaredOutcomeOrderRatherThanAlphabet() {
        var result = scoring.personality(List.of("ZEBRA", "ALPHA"), List.of(
                new ScoringService.PersonalityVote("ALPHA", 2),
                new ScoringService.PersonalityVote("ZEBRA", 2)));
        assertThat(result).isEqualTo("ZEBRA");
    }

    @Test
    void allZeroVotesStillHaveDeterministicResult() {
        assertThat(scoring.personality(List.of("FIRST", "SECOND"),
                List.of(new ScoringService.PersonalityVote("SECOND", 0)))).isEqualTo("FIRST");
    }

    @Test
    void unknownOutcomeIsRejectedInsteadOfIgnored() {
        assertThatThrownBy(() -> scoring.personality(List.of("FIRST", "SECOND"),
                List.of(new ScoringService.PersonalityVote("FOREIGN", 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
````

### src/test/resources/application-test.yml

````yaml
spring:
  datasource:
    url: jdbc:h2:mem:whichone-tests;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
whichone:
  jwt:
    # A test-only value; it must never be used in an Internet deployment.
    secret: test-only-signing-key-for-whichone-tests-2026
````
