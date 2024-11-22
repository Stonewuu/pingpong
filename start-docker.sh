#!/bin/bash

# 构建项目
echo "Building project..."
mvn clean package -DskipTests

# 启动Docker服务
echo "Starting Docker services..."
docker-compose up --build -d

# 等待服务启动
echo "Waiting for services to start..."
sleep 30

# 检查服务状态
echo "Checking service status..."
curl http://localhost:8080/actuator/health

echo "Setup complete!" 