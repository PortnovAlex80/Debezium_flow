#!/bin/bash


//systemctl restart docker
docker ps

docker stop mailhog
docker rm mailhog
docker start mailhog
docker run -d -p 1025:1025 -p 8025:8025 --name mailhog mailhog/mailhog

docker stop my-postgres-container
docker rm my-postgres-container
docker run -d --name my-postgres-container -e POSTGRES_PASSWORD=aaa -e POSTGRES_USER=aaa -e POSTGRES_DB=PepsiSales -p 5432:5432 postgres:9.4

docker ps

#mvn -Dninja.jvmArgs="-DoutputToConsole=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" clean compile install  ninja:run -DskipTests -Dninja.mode=dev