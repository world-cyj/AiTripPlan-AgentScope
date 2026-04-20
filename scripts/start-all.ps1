param(
    [string]$NacosServerAddr = "localhost:8848",
    [bool]$EnableA2A = $false,
    [string]$MavenRepoLocal = "",
    [int]$ManagerPort = 8081,
    [int]$RoutePort = 8082,
    [int]$TripPort = 8085
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

$env:NACOS_SERVER_ADDR = $NacosServerAddr
$env:A2A_SERVER_ENABLED = if ($EnableA2A) { "true" } else { "false" }

if ([string]::IsNullOrWhiteSpace($MavenRepoLocal)) {
    $MavenRepoLocal = Join-Path $root ".m2repo"
}
New-Item -ItemType Directory -Force $MavenRepoLocal | Out-Null

Write-Host "NACOS_SERVER_ADDR=$($env:NACOS_SERVER_ADDR), A2A_SERVER_ENABLED=$($env:A2A_SERVER_ENABLED)"
Write-Host "Maven local repo: $MavenRepoLocal"

function Test-PortInUse([int]$Port) {
    try {
        return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop | Select-Object -First 1)
    } catch {
        return $false
    }
}

function Stop-ProcessByPort([int]$Port) {
    $netstat = netstat -ano | Select-String ":$Port"
    if (-not $netstat) {
        return
    }
    $pids = @()
    foreach ($line in $netstat) {
        $parts = ($line.ToString() -replace "\s+", " ").Trim().Split(" ")
        if ($parts.Length -ge 5) {
            $pid = $parts[$parts.Length - 1]
            if ($pid -match "^\d+$" -and $pid -ne "0") {
                $pids += [int]$pid
            }
        }
    }
    $pids = $pids | Select-Object -Unique
    foreach ($pid in $pids) {
        try {
            Stop-Process -Id $pid -Force -ErrorAction Stop
            Write-Host "Stopped process on port $Port (PID=$pid)."
        } catch {
            Write-Host "Failed to stop PID=$pid on port ${Port}: $($_.Exception.Message)"
        }
    }
}

Write-Host "Cleaning old manager process on port $ManagerPort (if any)..."
Stop-ProcessByPort -Port $ManagerPort

$ports = @($ManagerPort, $RoutePort, $TripPort)
foreach ($p in $ports) {
    if (Test-PortInUse -Port $p) {
        if ($p -eq $ManagerPort) {
            throw "Port $p is already in use. Stop the existing process first."
        }
        Write-Host "Port $p already in use, will reuse existing service."
    }
}

Write-Host "Installing shared module artifacts..."
mvn "-Dmaven.repo.local=$MavenRepoLocal" -DskipTests -pl commons -am install

Write-Host "Starting manager_agent on $ManagerPort..."
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root'; mvn ""-Dmaven.repo.local=$MavenRepoLocal"" -f manager_agent/pom.xml spring-boot:run ""-Dspring-boot.run.arguments=--server.port=$ManagerPort"""

if (-not (Test-PortInUse -Port $RoutePort)) {
    Write-Host "Starting routeMaking_agent on $RoutePort..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root'; mvn ""-Dmaven.repo.local=$MavenRepoLocal"" -f routeMaking_agent/pom.xml spring-boot:run ""-Dspring-boot.run.arguments=--server.port=$RoutePort"""
} else {
    Write-Host "Skip routeMaking_agent start, service already listening on $RoutePort."
}

if (-not (Test-PortInUse -Port $TripPort)) {
    Write-Host "Starting tripPlanner_agent on $TripPort..."
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root'; mvn ""-Dmaven.repo.local=$MavenRepoLocal"" -f tripPlanner_agent/pom.xml spring-boot:run ""-Dspring-boot.run.arguments=--server.port=$TripPort"""
} else {
    Write-Host "Skip tripPlanner_agent start, service already listening on $TripPort."
}

Write-Host "All start commands submitted."
Write-Host "Frontend URL: http://localhost:$ManagerPort/"
