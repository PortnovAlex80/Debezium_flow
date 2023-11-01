FROM debezium/connect:latest

USER root

# Скачивание и установка PostgreSQL JDBC драйвера
ARG POSTGRES_VERSION=42.2.8
RUN cd /kafka/libs && curl -sO https://jdbc.postgresql.org/download/postgresql-$POSTGRES_VERSION.jar

# Скачивание и установка confluent JDBC connector
RUN mkdir /kafka/connect/jdbc && cd /kafka/connect/jdbc &&\
    curl -sO https://packages.confluent.io/maven/io/confluent/kafka-connect-jdbc/10.2.0/kafka-connect-jdbc-10.2.0.jar


USER kafka
