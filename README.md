# О проекте

### Рекомендательный сервис книг.
На данный момент реализовано:

* Добавление новой книги
* Просмотр списка книг и сортировка по жанру
* Для "смотрибельности" и чтоб избавиться от пустоты на главную страницу парсятся цитаты с bash.org.ru
* Механизм регистрации и авторизации.
### В проекте используется

* Spring Boot, Spring Security
* Lombok
* PostgreSQL
* jSoup

### В проекте используется
В дальнейшем планируется сделать следующее:
* добавить возможность оценивать книги для зарегистрированных и авторизованных пользователей
* полноценный личный кабинет для пользователей с историей оценок по книгам
* подружить проект с каким-нибудь брокером сообщений
* проработать вопросы кэширования данных в БД
* настроить интеграцию с рекомендательной системой, написанной на python.