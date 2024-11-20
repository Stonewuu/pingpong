FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制服务jar包
COPY ping-service/target/*.jar ping-service.jar
COPY pong-service/target/*.jar pong-service.jar

# 复制启动脚本
COPY start-services.sh .
RUN chmod +x start-services.sh

ENV JAVA_OPTS="-Xmx256m"

CMD ["./start-services.sh"] 