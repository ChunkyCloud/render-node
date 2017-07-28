#!/bin/bash
docker build -t build-img -f Dockerfile.build .
docker create --name build-cont build-img

mkdir -p target
docker cp build-cont:/usr/src/app/target ./
docker rm build-cont

docker build -t wertarbyte/rs3-rendernode .
