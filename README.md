# Spring Boot — WhichOne, итоговый проект

Отдельный Spring-проект API викторин: регистрация/JWT, создание, прохождение, история и статистика. Не продолжение Bookstore.

Маршрут: прочитать условия → установить зависимости → запустить → пройти проверки → подготовить к сдаче.

## Условия

- [final-backend-whichone.md](docs/materials/homework-2026-10-03/assignments/final-backend-whichone.md)

## Запуск из чистой копии

Нужен JDK 17 или 21 и интернет для первой загрузки Maven Wrapper/зависимостей. Откройте терминал в корне репозитория рядом с pom.xml.

macOS/Linux:

~~~bash
java -version
bash ./mvnw spring-boot:run
~~~

Windows PowerShell:

~~~powershell
java -version
.\mvnw.cmd spring-boot:run
~~~

Остановка: Ctrl+C. Адрес и настройки приведены в исходном руководстве ниже.

Если нужен внешний сервис, скопируйте .env.example в .env и заполните параметры по руководству. Настоящий .env не включён. Для demo используйте параметры исходного руководства.

## Проверка

~~~bash
bash ./mvnw clean verify
~~~

Windows PowerShell: .\mvnw.cmd clean verify.

В этой упаковке установки и проверки не выполнялись. Сборка, браузер, macOS и реальное устройство имеют отдельный статус; наличие файлов не означает готовность к сдаче.

## Разбор и материалы

- [Исходное руководство приложения](docs/PROJECT-README.md)


## Зависимости и открытые проверки

- По умолчанию локальная H2; PostgreSQL/Docker — отдельный профиль. Рабочая база не копируется.


## Единый запуск

```bash
bash ./build-and-run.sh --list
bash ./build-and-run.sh
```

[Запуск и перенос в свой GitHub](docs/CLIENT-HANDOFF.md).

<!-- upstream-tools:start -->
## Скачать независимую копию

В папке `delivery-tools` находятся скрипт, Python companion и каталог всех 24 проектов.
Нужны Git, Python 3.9+ и Bash 3.2+ (macOS/Linux или Windows через WSL/Git Bash).

```bash
mkdir -p "$HOME/Projects"
bash delivery-tools/export-project.sh --catalog delivery-tools/projects.tsv --project spring-whichone --dest "$HOME/Projects/polina-whichone" --init-git
```

Результат: отдельная папка с новым Git без исходной истории и remote. Без
`--init-git` Git metadata отсутствует. [Все параметры](delivery-tools/README.md).
Отмеченный downloader и этот блок удаляются из чистой копии автоматически.
<!-- upstream-tools:end -->
