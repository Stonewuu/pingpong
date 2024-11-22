#!/bin/sh

# 启动三个ping服务实例
java $JAVA_OPTS \
  -DSPRING_APPLICATION_NAME=ping-service \
  -DSPRING_APPLICATION_INSTANCE_ID=ping-1 \
  -DSERVER_PORT=8081 \
  -DPONG_SERVICE_URL=$PONG_SERVICE_URL \
  -jar ping-service.jar &

java $JAVA_OPTS \
  -DSPRING_APPLICATION_NAME=ping-service \
  -DSPRING_APPLICATION_INSTANCE_ID=ping-2 \
  -DSERVER_PORT=8082 \
  -DPONG_SERVICE_URL=$PONG_SERVICE_URL \
  -jar ping-service.jar &

java $JAVA_OPTS \
  -DSPRING_APPLICATION_NAME=ping-service \
  -DSPRING_APPLICATION_INSTANCE_ID=ping-3 \
  -DSERVER_PORT=8083 \
  -DPONG_SERVICE_URL=$PONG_SERVICE_URL \
  -jar ping-service.jar &

# 保持容器运行
wait 