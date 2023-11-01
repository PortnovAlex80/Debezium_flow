#!/bin/bash

docker ps

docker stop mailhog
docker rm mailhog


docker stop my-postgres-container
docker rm my-postgres-container

docker ps