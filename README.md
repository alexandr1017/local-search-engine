# localSearchEngine
<img src="src/main/resources/static/assets/img/icons/house-svgrepo-com.svg" width="200" height="200">
<img src="src/main/resources/static/assets/img/icons/research-svgrepo-com.svg"width="200" height="150">
<img src="src/main/resources/static/assets/img/icons/searchEngine.png"width="400">

Это учебный проект локального поискового движка по заданным сайтам. Движок предназначен для поиска страниц с информацией по ключевым словам и анализа сайтов.

## Оглавление
- [Технологии](#использованные-технологии)
- [Использование визуального интерфейса](#использование-визуального-интерфейса)
- [API](#api)
    - [Индексация](#индексация)
    - [Поиск](#поиск)
    - [Статистика](#статистика)
- [Контакты](#контакты)

## Использованные технологии

- <img src="https://simpleicons.org/icons/springboot.svg" width="20" height="20"> В проекте использован Spring Boot.
- <img src="https://simpleicons.org/icons/mysql.svg" width="20" height="20"> База данных: MySQL.
- <img src="https://simpleicons.org/icons/hibernate.svg" width="20" height="20"> ORM: Hibernate.
- <img src="src/main/resources/static/assets/img/icons/gears.svg" width="20" height="20"> Лемматизатор: Russian & English Morphology for Apache Lucene.

## Использование визуального интерфейса

1. Укажите сайты для индексации через конфигурационный файл **application.yaml** (не забудьте указать логин и пароль базы данных).
2. Запустите программу.
3. Перейдите во вкладку "**Managment**" и нажмите на кнопку "**Start indexing**".
4. После окончания процесса индексации статистическая информация станет доступна во вкладке "**Statistics**".
5. Для поиска используйте вкладку "**Search**", при использовании поиска есть возможность использовать ограничение на поиск по одному сайту.

[![Показать демонстрацию](https://img.shields.io/badge/%D0%9F%D0%BE%D0%BA%D0%B0%D0%B7%D0%B0%D1%82%D1%8C-%D0%B4%D0%B5%D0%BC%D0%BE%D0%BD%D1%81%D1%82%D1%80%D0%B0%D1%86%D0%B8%D1%8E-green)](src/main/resources/static/assets/img/icons/clip.gif)

## API

### Индексация
Доступные endpoints:

```html
GET /api/startIndexing -начать индексацию
```
```html
GET /api/stopIndexing -остановить индексацию
```
```html
POST /api/indexPage?url=URL страницы для добавления в индекс
```

### Поиск

```html
GET /api/search?query=тело запроса&site=ограничение на поиск внутри определенного сайта&offset=0&limit=10
```


### Статистика

```html
GET /api/statistics
```
Статистика выдается в виде JSON:
```JSON
{
  "result": true,
  "statistics": {
    "total": {
      "sites": 2,
      "pages": 230,
      "lemmas": 7759,
      "indexing": false
    },
    "detailed": [
      {
        "url": "http://www.playback.ru",
        "name": "PlayBack.Ru",
        "status": "INDEXED",
        "statusTime": 1692088027445,
        "error": "",
        "pages": 78,
        "lemmas": 1557
      },
      {
        "url": "https://et-cetera.ru",
        "name": "Театр «Et Cetera»",
        "status": "INDEXED",
        "statusTime": 1692088122082,
        "error": "",
        "pages": 152,
        "lemmas": 6202
      }
    ]
  }
}
```

### Контакты:
email: 79203430120@ya.ru
