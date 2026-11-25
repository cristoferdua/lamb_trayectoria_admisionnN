param([switch]$Recompilar)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$runDir = Join-Path $PSScriptRoot 'run'
New-Item -ItemType Directory -Force $runDir | Out-Null

$jvm = @('-Xms64m','-Xmx256m','-XX:+UseSerialGC','-XX:TieredStopAtLevel=1','-Dfile.encoding=UTF-8')

$servicios = @(
    @{ Nombre='lamb-config';         Dir='infra\lamb-config';          Puerto=18888; Health='http://localhost:18888/actuator/health' },
    @{ Nombre='lamb-eureka';         Dir='infra\lamb-eureka';          Puerto=18761; Health='http://localhost:18761/actuator/health' },
    @{ Nombre='lamb-admision-ms';    Dir='services\lamb-admision-ms';  Puerto=8084;  Health='http://localhost:8084/actuator/health' },
    @{ Nombre='lamb-trayectoria-ms'; Dir='services\lamb-trayectoria-ms';Puerto=8089; Health='http://localhost:8089/actuator/health' },
    @{ Nombre='lamb-gateway';        Dir='infra\lamb-gateway';         Puerto=18080; Health='http://localhost:18080/actuator/health' }
)

function Esperar-Url([string]$url,[int]$segundos=180) {
    $limite=(Get-Date).AddSeconds($segundos)
    while((Get-Date) -lt $limite){
        try { Invoke-RestMethod -Uri $url -TimeoutSec 3 | Out-Null; return $true } catch { Start-Sleep -Seconds 2 }
    }
    return $false
}
function Puerto-Ocupado([int]$p){ return [bool](Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue) }

Write-Host '== Infra docker (kafka, keycloak, postgres admision/trayectoria)' -ForegroundColor Cyan
docker compose -f (Join-Path $raiz 'kafka\compose-dev.yml') up -d
docker compose -f (Join-Path $raiz 'keycloak\compose-dev.yml') up -d
foreach ($ms in 'admision','trayectoria') {
    docker compose -f (Join-Path $raiz "services\lamb-$ms-ms\compose-dev.yml") up -d
}

foreach ($s in $servicios) {
    $dir = Join-Path $raiz $s.Dir
    $jar = Get-ChildItem (Join-Path $dir 'target') -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    if ($Recompilar -or -not $jar) {
        Write-Host "== Compilando $($s.Nombre)" -ForegroundColor Cyan
        Push-Location $dir; & .\mvnw.cmd -q -DskipTests package; Pop-Location
        $jar = Get-ChildItem (Join-Path $dir 'target') -Filter '*.jar' | Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    }
    $s.Jar = $jar.FullName
}

foreach ($s in $servicios) {
    if (Puerto-Ocupado $s.Puerto) { Write-Host "== $($s.Nombre) ya corre en $($s.Puerto)" -ForegroundColor Yellow; continue }
    Write-Host "== Arrancando $($s.Nombre) ($($s.Puerto))" -ForegroundColor Cyan
    $dir = Join-Path $raiz $s.Dir
    $argsJava = ($jvm + @('-jar',"`"$($s.Jar)`"")) -join ' '
    Start-Process java -ArgumentList $argsJava -WorkingDirectory $dir -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runDir "$($s.Nombre).out") `
        -RedirectStandardError (Join-Path $runDir "$($s.Nombre).err") | Out-Null
    if ($s.Nombre -in 'lamb-config','lamb-eureka') {
        if (-not (Esperar-Url $s.Health 180)) { throw "$($s.Nombre) no respondio" }
    }
}

Write-Host '== Esperando health de todos...' -ForegroundColor Cyan
foreach ($s in $servicios) {
    $ok = Esperar-Url $s.Health 240
    $estado = if ($ok) { 'UP' } else { 'SIN RESPUESTA' }
    $color  = if ($ok) { 'Green' } else { 'Red' }
    Write-Host ("{0,-24} {1,-6} {2}" -f $s.Nombre,$s.Puerto,$estado) -ForegroundColor $color
}
Write-Host 'Listo.' -ForegroundColor Green
