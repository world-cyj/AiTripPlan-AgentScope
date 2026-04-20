param(
    [string]$ManagerBaseUrl = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"

Write-Host "1) Check ping..."
$ping = Invoke-RestMethod -Uri "$ManagerBaseUrl/app/ping" -Method Get
Write-Host "Ping response: $ping"

Write-Host "2) Call planning API..."
$body = @{
    requestId = "smoke-001"
    origin = "Shenzhen"
    destination = "Huizhou"
    travelDate = "2026-05-01"
    preferences = "nature, food"
    budget = "1500"
    transportMode = "self-driving"
    extraRequirements = "family friendly"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$ManagerBaseUrl/app" -Method Post -ContentType "application/json" -Body $body

Write-Host "success=$($response.success)"
Write-Host "message=$($response.message)"
Write-Host "degraded=$($response.degraded)"
Write-Host "idempotentHit=$($response.idempotentHit)"
Write-Host "workflowId=$($response.workflowId)"

if ($response.taskRecords) {
    Write-Host "3) Task records..."
    foreach ($task in $response.taskRecords) {
        Write-Host " - taskId=$($task.taskId), role=$($task.role), status=$($task.status), attempts=$($task.attempts)"
    }
}
