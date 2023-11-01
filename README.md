# Debezium_flow
The project simulates the process of system refactoring, including data migration from an outdated database (legacy DB) to a modern DB. Utilizing Debezium Kafka Connector, the project aims to prepare for the transition from monolithic architecture to a more flexible microservices and event-driven architecture.

### Раздел 1: Подготовительные работы

Образец: https://github.com/dursunkoc/kafka_connect_sample OracleDB

1. **Настроить docker-compose.yml**

   Он должен содержать:
   - Образы БД
   - Debezium, Zookeeper, Kafka
   - Отдельно создать dockerfile для скачивания и установки Debezium JDBC connector

   ```yaml
   FROM debezium/connect:latest
   
   USER root
   
   # Скачивание и установка PostgreSQL JDBC драйвера
   ARG POSTGRES_VERSION=42.2.8
   RUN cd /kafka/libs && curl -sO https://jdbc.postgresql.org/download/postgresql-$POSTGRES_VERSION.jar
   
   # Скачивание и установка confluent JDBC connector
   RUN mkdir /kafka/connect/jdbc && cd /kafka/connect/jdbc &&\
   curl -sO https://packages.confluent.io/maven/io/confluent/kafka-connect-jdbc/10.2.0/kafka-connect-jdbc-10.2.0.jar
   
   
   USER kafka
   ```

   Добавить log4j.properties для логов kafka, если будет использовать Consumer или Producer дополнительно к Debezium
   ```yaml
      # Установка уровня логирования и appender для корневого логгера
      log4j.rootLogger=INFO, stdout
      
      # Указание appender'а и его layout
      log4j.appender.stdout=org.apache.log4j.ConsoleAppender
      log4j.appender.stdout.Target=System.out
      log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
      log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n
   ```

   Если предполагается использование Kafka Connect от Confluent, потребуется самостоятельно добавить плагины необходимых коннекторов в директорию, указанную в plugin.path или задаваемую через переменную окружения CLASSPATH. 
   Настройки воркера Kafka Connect и коннекторов определяются через конфигурационные файлы, которые передаются аргументами к команде запуска воркера. Подробнее см. в документации. https://docs.confluent.io/platform/current/connect/userguide.html

   Статьи на хабре: https://habr.com/ru/companies/flant/articles/523510/

### Раздел 2: Проверенный чек лист

Запуск системы:
   ```bash
   docker stop $(docker ps -aq)
   docker rm $(docker ps -aq)
   docker-compose build --no-cache 
   docker-compose up -d
   ```
   Примечание: docker-compose build --no-cache нужен, если обновили Dockerfile

1. **Проверка контейнеров Docker**:
    ```bash
    docker ps
    ```

    ```bash
    docker-compose ps
    ```

2. **Проверка баз данных (old-db и new-db)**:
    - Для `old-db`:
        ```bash
        docker exec -it olddb_container psql -U postgres -d olddb
        ```
    - Для `new-db`:
        ```bash
        docker exec -it newdb_container psql -U postgres -d newdb
        ```

3. **Проверка таблиц в базах данных**:
    - Внутри каждой сессии psql выполните:
        ```sql
        \dt
        ```
      Эта команда покажет список всех таблиц в текущей базе данных.

4. **Проверка режима WAL**:
    - Проверьте текущий режим WAL (Write-Ahead Logging) в каждой сессии psql:
        ```sql
         SHOW wal_level;
        ```
    - Если уровень WAL не `logical`, измените его:
        ```sql
        ALTER SYSTEM SET wal_level = 'logical';
        SELECT pg_reload_conf();
        ```
    - **Примечание**: Для сохранения изменений после перезагрузки, убедитесь, что используется постоянный объем (volume) для данных PostgreSQL. В противном случае, изменения могут быть потеряны.

      - Конфигурация `old-db` docker-compose.yaml:
        ```yaml
        old-db:
          image: postgres:latest
          volumes:
            - ./old-db-data:/var/lib/postgresql/data
        ```
 Добавить проверку прав пользоватлей :

5. **Проверка Debezium Connect**:
    
    - Проверка состояния Debezium Connect:
        ```bash
        curl -X GET http://localhost:8083/
        ```
    - Проверка наличия коннекторов:
        ```bash
        curl -i -X GET http://localhost:8083/connectors
        ```
    - Создание source-коннектора:
        ```bash
        curl -X POST -H "Content-Type: application/json" --data '{
        "name": "postgres-connector",
        "config": {
        "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "tasks.max": "1",
        "database.hostname": "olddb_container",
        "database.port": "5432",
        "database.user": "postgres",
        "database.password": "postgres",
        "database.dbname": "olddb",
        "database.server.name": "postgres-server",
        "table.include.list": "public.users",
        "topic.prefix": "users_topic_prefix", 
        "database.history.kafka.bootstrap.servers": "kafka:9092",
        "database.history.kafka.topic": "schema-changes.users",
        "plugin.name": "pgoutput" }
        }' http://localhost:8083/connectors/ 
        ```
    - Проверка статуса коннекторов:
   ```bash
        curl -s "http://localhost:8083/connectors?expand=info&expand=status"
   ```

   - Настройка SINK connectors
   https://habr.com/ru/companies/vk/articles/529484/#9

   ```bash
      curl -X POST -H "Content-Type: application/json" --data '{
      "name": "jdbc-sink-newdb",
      "config": {
      "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",
      "tasks.max": "1",
      "dialect.name": "PostgreSqlDatabaseDialect",
      "table.name.format": "users",
      "topics": "users_topic_prefix.public.users",
      "connection.url": "jdbc:postgresql://newdb_container:5432/newdb",
      "connection.user": "postgres",
      "connection.password": "postgres",  
      "transforms": "unwrap",
      "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
      "transforms.unwrap.drop.tombstones": "false",      
      "auto.create": "true",
      "auto.evolve": "true",
      "insert.mode": "upsert",
      "pk.mode": "record_key",
      "pk.fields": "id",
      "primary.key.mode": "record_key",
      "primary.key.fields": "id",
      "delete.enabled": "true"
      }
      }' http://localhost:8083/connectors/
      ```
      - Статус SINK connectors
      ```bash
               curl http://localhost:8083/connectors/jdbc-sink-newdb/status
      ```

6. **Проверка Kafka и Zookeeper**:
    - Проверка списка топиков Kafka:
        ```bash
        docker exec -it kafka bash
        cd /usr/bin
       ./kafka-topics --list --bootstrap-server localhost:9092
        ```
      ./kafka-console-consumer --bootstrap-server localhost:9092 --topic users_topic_prefix.public.users --from-beginning
      ./kafka-topics --list --bootstrap-server localhost:9092
      ./kafka-consumer-groups --bootstrap-server localhost:9092 --list

      Обратите внимание на примечание: коннектор создает в Kafka отдельный топик под каждую таблицу (one topic per table).

7. **Запуск логов Consumer на определенную таблицу**:
    ```bash
    ./kafka-console-consumer --bootstrap-server localhost:9092 --topic users_topic_prefix.public.users --from-beginning
    ```
   Обратите внимание на примечание:наименование топика users_topic_prefix должно совпадать с указанным наименованием в настройках коннектора  
    ```json
        {"topic.prefix": "users_topic_prefix"}     
     ```

---

### Раздел 3: Опциональные Шаги (еще не проверенные, взяты из репозитория https://github.com/dursunkoc/kafka_connect_sample)

8. **Проверка файлов конфигурации**:
    - Убедитесь, что все необходимые конфигурационные файлы на месте:
        ```bash
        ls -l /path/to/your/config/files/
        ```

9. **Дополнительные операции с Kafka и Connect**:
    - Инспекция топика Kafka:
        ```bash
        export DEBEZIUM_VERSION=1.7
        export PROJECT_PATH=$(pwd -P)
        docker-compose -f docker-compose.yaml exec kafka /kafka/bin/kafka-console-consumer.sh \
            --bootstrap-server kafka:9092 \
            --from-beginning \
            --property print.key=true \
            --topic oracle-db-source.INVENTORY.CUSTOMERS
        ```
    - Управление коннекторами:
        - Просмотр статуса коннекторов:
            ```bash
            curl -s "http://localhost:8083/connectors?expand=info&expand=status"
            ```
        - Перезапуск коннектора:
            ```bash
            curl -i -X POST  http://localhost:8083/connectors/inventory-source-connector/restart
            ```
          Или:
            ```bash
            curl -i -X POST  http://localhost:8083/connectors/jdbc-sink-customers/restart
            ```
        - Удаление коннектора:
            ```bash
            curl -i -X DELETE  http://localhost:8083/connectors/postgres-connector
            ```
          Или:
            ```bash
            curl -i -X DELETE  http://localhost:8083/connectors/jdbc-sink-newdb
            ```

10. **Остановка топологии**:
   ```bash
       export DEBEZIUM_VERSION=1.7
       export PROJECT_PATH=$(pwd -P
   
   )
   docker-compose -f docker-compose.yaml down
  ```

   Удаление SINK connectors
      ```bash
      curl -i -X DELETE  http://localhost:8083/connectors/jdbc-sink-newdb
      ```
   
   Проверка логов SINK connectors
      ```bash
      docker logs debezium | grep "JdbcSinkConnector"
   ```

---

   ## Пример работы с двумя портами на Kafka
      
   ```yaml
           kafka:
             image: confluentinc/cp-kafka:latest
             depends_on:
               - zookeeper
             ports:
               - "9092:9092"
               - "9093:9093"
             environment:
               KAFKA_BROKER_ID: 1
               KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
               KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka:9092,EXTERNAL://localhost:9093
               KAFKA_LISTENERS: INTERNAL://0.0.0.0:9092,EXTERNAL://0.0.0.0:9093
               KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
               KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
               KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
             container_name: kafka
         
           debezium:
             build: .
             depends_on:
               - kafka
             ports:
               - "8083:8083"
             environment:
               BOOTSTRAP_SERVERS: kafka:9092
               GROUP_ID: 1
               CONFIG_STORAGE_TOPIC: my_connect_configs
               OFFSET_STORAGE_TOPIC: my_connect_offsets
               STATUS_STORAGE_TOPIC: my_connect_statuses
             container_name: debezium
   ```

   ## Пример записи vol БД в текущую папку проекта
   
   ```yaml
         services:
         old-db:
            image: postgres:latest
            volumes:
               #      - ./old-db-data:/var/lib/postgresql/data
               - ${PWD}/old-db-data:/var/lib/postgresql/data
            ports:
               - "5432:5432"
            environment:
               POSTGRES_USER: postgres
               POSTGRES_PASSWORD: postgres
               POSTGRES_DB: olddb
            container_name: olddb_container

         new-db:
            image: postgres:latest
            volumes:
               #      - new-db-data:/var/lib/postgresql/data
               - ${PWD}/new-db-data:/var/lib/postgresql/data
            ports:
               - "5433:5432"
            environment:
               POSTGRES_USER: postgres
               POSTGRES_PASSWORD: postgres
               POSTGRES_DB: newdb
            container_name: newdb_container
   ```
