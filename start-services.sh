#!/bin/sh

# 启动pong服务
java $JAVA_OPTS \
  -DSPRING_APPLICATION_NAME=pong-service \
  -DSPRING_APPLICATION_INSTANCE_ID=pong-1 \
  -jar pong-service.jar &

# 等待pong服务启动
sleep 5

# 启动三个ping服务实例
java $JAVA_OPTS \
  -DSPRING_APPLICATION_NAME=ping-service \
  -DSPRING_APPLICATION_INSTANCE_ID=ping-1 \
  -DSERVER_PORT=8080 \
  -DPONG_SERVICE_URL=http://localhost:8081 \
  -jar ping-service.jar &

java $JAVA_OPTS \
  -DSPRING_APPLICATION_NAME=ping-service \
  -DSPRING_APPLICATION_INSTANCE_ID=ping-2 \
  -DSERVER_PORT=8082 \
  -DPONG_SERVICE_URL=http://localhost:8081 \
  -jar ping-service.jar &

java $JAVA_OPTS \
  -DSPRING_APPLICATION_NAME=ping-service \
  -DSPRING_APPLICATION_INSTANCE_ID=ping-3 \
  -DSERVER_PORT=8083 \
  -DPONG_SERVICE_URL=http://localhost:8081 \
  -jar ping-service.jar &

# 保持容器运行
wait 