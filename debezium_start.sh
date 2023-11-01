docker stop $(docker ps -aq)

docker rm $(docker ps -aq)
#docker-compose build --no-cache

docker-compose up -d

