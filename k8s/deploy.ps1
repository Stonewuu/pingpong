
# 确保minikube在运行
Write-Host "检查minikube状态..." -ForegroundColor Yellow
$minikubeStatus = minikube status --format='{{.Host}}' 2>&1
if ($minikubeStatus -ne "Running") {
    Write-Host "启动minikube..." -ForegroundColor Yellow
    minikube start --memory=4096 --cpus=4
}

# 设置使用minikube的docker环境
Write-Host "配置Docker环境..." -ForegroundColor Yellow
& minikube -p minikube docker-env --shell powershell | Invoke-Expression

# 构建项目
Write-Host "构建项目..." -ForegroundColor Green
mvn clean package -DskipTests

# 构建Docker镜像
Write-Host "构建Docker镜像..." -ForegroundColor Green
docker build -t ping-service:latest -f Dockerfile.ping .
docker build -t pong-service:latest -f Dockerfile.pong .

# 创建命名空间
Write-Host "创建命名空间..." -ForegroundColor Yellow
kubectl create namespace pingpong --dry-run=client -o yaml | kubectl apply -f -

# 应用k8s配置
Write-Host "部署基础设施..." -ForegroundColor Green
kubectl apply -k k8s/base

# 等待PostgreSQL启动
Write-Host "等待PostgreSQL启动..." -ForegroundColor Yellow
kubectl -n pingpong wait --for=condition=ready pod -l app=postgres --timeout=10s

# 启动PostgreSQL端口转发
Write-Host "启动PostgreSQL端口转发..." -ForegroundColor Yellow
Start-Process kubectl -ArgumentList "port-forward -n pingpong svc/postgres 5432:5432" -WindowStyle Hidden

# 等待RocketMQ启动
Write-Host "等待RocketMQ启动..." -ForegroundColor Yellow
kubectl -n pingpong wait --for=condition=ready pod -l app=rocketmq-namesrv --timeout=10s
kubectl -n pingpong wait --for=condition=ready pod -l app=rocketmq-broker --timeout=10s

# 等待应用服务启动
Write-Host "等待应用服务启动..." -ForegroundColor Yellow
kubectl -n pingpong wait --for=condition=ready pod -l app=pong-service --timeout=10s
kubectl -n pingpong wait --for=condition=ready pod -l app=ping-service --timeout=10s

# 获取服务状态
Write-Host "`n服务状态:" -ForegroundColor Cyan
kubectl get pods -n pingpong

Write-Host "服务已在后台启动，请使用以下命令查看地址：" -ForegroundColor Green
Write-Host "minikube service -n pingpong pong-service --url" -ForegroundColor Gray
Write-Host "minikube service -n pingpong ping-service --url" -ForegroundColor Gray

Write-Host "`n查看服务日志:" -ForegroundColor Cyan
Write-Host "kubectl -n pingpong logs -l app=pong-service  # Pong服务日志" -ForegroundColor Gray
Write-Host "kubectl -n pingpong logs -l app=ping-service  # Ping服务日志" -ForegroundColor Gray
Write-Host "kubectl -n pingpong logs -l app=postgres     # PostgreSQL日志" -ForegroundColor Gray
Write-Host "kubectl -n pingpong logs -l app=rocketmq-namesrv  # RocketMQ NameServer日志" -ForegroundColor Gray
Write-Host "kubectl -n pingpong logs -l app=rocketmq-broker   # RocketMQ Broker日志" -ForegroundColor Gray

Write-Host "`n部署完成!" -ForegroundColor Green
