# PingPong 微服务示例

一个基于Spring Boot的微服务示例项目。

## 项目说明

项目包含两个微服务：
- Ping服务：主动发送请求的客户端服务
- Pong服务：接收并响应请求的服务端

## 技术选型

- Java 17
- Spring Boot 3.3.5
- Spring WebFlux
- Spock (测试框架)
- Maven
- Docker

## 快速开始

1. 构建整个项目:
    mvn clean package

2. 启动Pong服务:
    cd pong-service
    mvn spring-boot:run

3. 启动Ping服务:
    cd ping-service
    mvn spring-boot:run

## 服务验证

测试Pong服务接口:
    curl http://localhost:8081/api/pong

响应示例:
    正常响应: {"message": "World", "status": 200}
    限流响应: {"message": "Rate limited by Pong service", "status": 429}

## 测试运行

执行测试:
    mvn test

查看测试报告:
    target/site/jacoco/index.html

## 日志说明

每个服务的日志都存放在各自的目录下:

### Ping 服务日志 (logs/ping-service/)
- 应用日志: application-${instanceId}.log
- 审计日志: audit-${instanceId}.log
- 限流日志: rate-limit-${instanceId}.log

### Pong 服务日志 (logs/pong-service/)
- 应用日志: application-${instanceId}.log
- 审计日志: audit-${instanceId}.log
- 限流日志: rate-limit-${instanceId}.log

注: ${instanceId} 为服务实例ID，在多实例部署时用于区分不同实例的日志

## Docker部署

- 构建项目和镜像
   - 在项目根目录执行: mvn clean package
   - 构建Docker镜像: docker-compose build

- 启动服务集群
   - 启动所有服务: docker-compose up -d
   - 查看服务状态: docker-compose ps
   - 查看服务日志: docker-compose logs -f
   - 停止所有服务: docker-compose down

- 日志查看
   所有服务的日志统一收集到项目根目录的logs文件夹下：
   - Ping服务日志: logs/ping-service/
   - Pong服务日志: logs/pong-service/
