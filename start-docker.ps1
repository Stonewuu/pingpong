# 设置控制台输出编码为UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "Building project..." -ForegroundColor Green
mvn clean package -DskipTests

Write-Host "Starting Docker services..." -ForegroundColor Green
docker-compose up --build -d

Write-Host "Waiting for services to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host "Checking service status..." -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method Get
    Write-Host "Service health check response: $($response.Content)" -ForegroundColor Green
}
catch {
    Write-Host "Warning: Health check failed. Error: $_" -ForegroundColor Yellow
}

Write-Host "Setup complete!" -ForegroundColor Green

# 显示如何查看日志的提示
Write-Host "`nTo view service logs, use these commands:" -ForegroundColor Cyan
Write-Host "docker-compose logs postgres  # PostgreSQL logs" -ForegroundColor Gray
Write-Host "docker-compose logs namesrv   # RocketMQ NameServer logs" -ForegroundColor Gray
Write-Host "docker-compose logs broker    # RocketMQ Broker logs" -ForegroundColor Gray
Write-Host "docker-compose logs pong-service # Pong Service logs" -ForegroundColor Gray 