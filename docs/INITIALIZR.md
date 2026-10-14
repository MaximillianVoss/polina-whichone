# Происхождение каркаса WhichOne

2026-10-05 новый проект запрошен у https://start.spring.io/starter.zip с параметрами Maven / Java / Java17, groupId=com.whichone, artifactId=whichone, packageName=com.whichone, зависимости web, data-jpa, validation, security, oauth2-resource-server, h2, postgresql, flyway.

Прямой запрос bootVersion=3.5.16 получил HTTP400: Initializr принимает Spring Boot >=4.0.0. Затем успешно скачан и распакован новый каркас bootVersion=4.0.8.

Из свежего каркаса сохранены только стандартные Maven Wrapper mechanics. POM осознанно адаптирован на совместимую учебную среду Spring Boot3.5.16 / release17, обновлены имена starter зависимостей с Boot4 на Boot3, добавлен springdoc2.8.13. Прикладные классы, сущности, миграция, тесты и демонстрация написаны отдельно для WhichOne.

Bookstore не копировался. Каркас Initializr является новым проектом; переход версии записан явно, а не скрыт.
