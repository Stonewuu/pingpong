# 设置控制台编码
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# 停止端口转发
Write-Host "停止端口转发..." -ForegroundColor Yellow
Get-Process kubectl | Where-Object {$_.CommandLine -like "*port-forward*"} | Stop-Process -Force

# 停止 minikube service 隧道
Write-Host "停止服务隧道..." -ForegroundColor Yellow
Get-Process minikube | Where-Object {$_.CommandLine -like "*service*"} | Stop-Process -Force

# 删除所有资源
Write-Host "删除Kubernetes资源..." -ForegroundColor Yellow
kubectl delete -k k8s/base

# 清理 minikube tunnel
Write-Host "清理 minikube tunnel..." -ForegroundColor Yellow
minikube tunnel --cleanup

Write-Host "`n所有服务已停止!" -ForegroundColor Green 