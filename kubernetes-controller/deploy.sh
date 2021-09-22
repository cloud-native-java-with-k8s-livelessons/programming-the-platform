#!/usr/bin/env bash
NS=cnj
APP_NAME=kubernetes-controller
IMAGE_NAME=gcr.io/bootiful/${APP_NAME}:latest
./mvnw -DskipTests=true clean package spring-boot:build-image \
 -Dspring-boot.build-image.imageName=${IMAGE_NAME}
docker push $IMAGE_NAME
