# Arranca todo el Modulo 02 para la demo S6-S9 y abre las paginas de evidencia.
# Uso:  powershell -ExecutionPolicy Bypass -File scripts\demo-start.ps1
#       -SinNavegador   no abre pestanas
#       -Oculto         corre los servicios sin ventanas (salida en scripts\run\*.out)
#       -Recompilar     vuelve a empaquetar los jar antes de arrancar
param(
    [switch]$SinNavegador,
    [switch]$Oculto,
    [switch]$Recompilar
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$runDir = Join-Path $PSScriptRoot 'run'
New-Item -ItemType Directory -Force $runDir | Out-Null

# Heap chico: son 9 JVM en la misma laptop
$jvm = @('-Xms64m', '-Xmx256m', '-XX:+UseSerialGC', '-XX:TieredStopAtLevel=1', '-Dfile.encoding=UTF-8')

$servicios = @(
    @{ Nombre = 'lamb-config';               Dir = 'infra\lamb-config';                  Puerto = 18888; Health = 'http://localhost:18888/actuator/health' },
    @{ Nombre = 'lamb-eureka';               Dir = 'infra\lamb-eureka';                  Puerto = 18761; Health = 'http://localhost:18761/actuator/health' },
    @{ Nombre = 'lamb-admision-ms';          Dir = 'services\lamb-admision-ms';          Puerto = 8084;  Health = 'http://localhost:8084/actuator/health' },
    @{ Nombre = 'lamb-curriculo-ms';         Dir = 'services\lamb-curriculo-ms';         Puerto = 8088;  Health = 'http://localhost:8088/actuator/health' },
    @{ Nombre = 'lamb-planificacion-ms';     Dir = 'services\lamb-planificacion-ms';     Puerto = 8083;  Health = 'http://localhost:8083/actuator/health' },
    @{ Nombre = 'lamb-evaluacion-ms';        Dir = 'services\lamb-evaluacion-ms';        Puerto = 8086;  Health = 'http://localhost:8086/actuator/health' },
    @{ Nombre = 'lamb-trayectoria-ms';       Dir = 'services\lamb-trayectoria-ms';       Puerto = 8089;  Health = 'http://localhost:8089/actuator/health' },
    @{ Nombre = 'lamb-gateway';              Dir = 'infra\lamb-gateway';                 Puerto = 18080; Health = 'http://localhost:18080/actuator/health' }
)

function Esperar-Url([string]$url, [int]$segundos = 120) {
    $limite = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $limite) {
        try {
            Invoke-RestMethod -Uri $url -TimeoutSec 3 | Out-Null
            return $true
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

function Puerto-Ocupado([int]$puerto) {
    return [bool](Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue)
}

# 1. Docker
Write-Host '== Docker' -ForegroundColor Cyan
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Docker Desktop apagado, iniciando...'
    Start-Process 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
    $limite = (Get-Date).AddMinutes(3)
    do {
        Start-Sleep -Seconds 5
        docker info *> $null
    } while ($LASTEXITCODE -ne 0 -and (Get-Date) -lt $limite)
    if ($LASTEXITCODE -ne 0) { throw 'Docker no arranco en 3 minutos' }
}

docker compose -f (Join-Path $raiz 'kafka\compose-dev.yml') up -d
# Keycloak reemplaza a lamb-auth-ms como emisor de tokens (puerto 18180, realm lamb)
docker compose -f (Join-Path $raiz 'keycloak\compose-dev.yml') up -d
foreach ($ms in 'admision', 'curriculo', 'planificacion', 'evaluacion', 'trayectoria') {
    docker compose -f (Join-Path $raiz "services\lamb-$ms-ms\compose-dev.yml") up -d
}

# 2. Jar de cada servicio
foreach ($s in $servicios) {
    $dir = Join-Path $raiz $s.Dir
    $jar = Get-ChildItem (Join-Path $dir 'target') -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    if ($Recompilar -or -not $jar) {
        Write-Host "== Compilando $($s.Nombre)" -ForegroundColor Cyan
        Push-Location $dir
        & .\mvnw.cmd -q -DskipTests package
        Pop-Location
        $jar = Get-ChildItem (Join-Path $dir 'target') -Filter '*.jar' | Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    }
    $s.Jar = $jar.FullName
}

# 3. Arranque en orden: config -> eureka -> servicios -> gateway
foreach ($s in $servicios) {
    if (Puerto-Ocupado $s.Puerto) {
        Write-Host "== $($s.Nombre) ya esta corriendo en $($s.Puerto)" -ForegroundColor Yellow
        continue
    }
    Write-Host "== Arrancando $($s.Nombre) ($($s.Puerto))" -ForegroundColor Cyan
    $dir = Join-Path $raiz $s.Dir
    $argsJava = ($jvm + @('-jar', "`"$($s.Jar)`"")) -join ' '
    if ($Oculto) {
        Start-Process java -ArgumentList $argsJava -WorkingDirectory $dir -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $runDir "$($s.Nombre).out") `
            -RedirectStandardError (Join-Path $runDir "$($s.Nombre).err") | Out-Null
    } else {
        # java.exe abre su propia consola con los logs del servicio
        Start-Process java -ArgumentList $argsJava -WorkingDirectory $dir | Out-Null
    }
    # config y eureka tienen que estar arriba antes de seguir
    if ($s.Nombre -in 'lamb-config', 'lamb-eureka') {
        if (-not (Esperar-Url $s.Health 120)) { throw "$($s.Nombre) no respondio" }
    }
}

Write-Host '== Esperando que todos respondan...' -ForegroundColor Cyan
foreach ($s in $servicios) {
    $ok = Esperar-Url $s.Health 180
    $estado = if ($ok) { 'UP' } else { 'SIN RESPUESTA' }
    $color = if ($ok) { 'Green' } else { 'Red' }
    Write-Host ("{0,-24} {1,-6} {2}" -f $s.Nombre, $s.Puerto, $estado) -ForegroundColor $color
}

# El Gateway tarda ~30 s en ver en su cache de Eureka a los servicios recien registrados.
# Sin token cada ruta protegida debe responder 401 (y no 503): asi se sabe que el Gateway ya enruta.
# (El login es con Keycloak + PKCE desde el frontend; el cliente lamb-frontend no acepta usuario/clave directo.)
foreach ($ruta in '/api/v1/postulaciones', '/api/v1/planes-estudio', '/api/v1/grupos', '/api/v1/actas', '/api/v1/trayectorias') {
    $limite = (Get-Date).AddSeconds(90)
    do {
        try { Invoke-WebRequest "http://localhost:18080$ruta" -UseBasicParsing -TimeoutSec 5 | Out-Null; $codigo = 200 }
        catch { $codigo = [int]$_.Exception.Response.StatusCode }
        if ($codigo -ne 401) { Start-Sleep -Seconds 3 }
    } while ($codigo -ne 401 -and (Get-Date) -lt $limite)
    Write-Host ("Gateway -> {0}: {1}" -f $ruta, $(if ($codigo -eq 401) { 'OK (401 sin token)' } else { "todavia no ($codigo)" }))
}
# planificacion refresca su lista de Eureka cada 5 s: margen para que Feign ya vea a curriculo
Start-Sleep -Seconds 10

# 4. Paginas de evidencia
if (-not $SinNavegador) {
    $paginas = @(
        'http://localhost:18761',                                         # Eureka: los 6 servicios registrados
        'http://localhost:18888/lamb-planificacion-ms/dev',              # Config Server: parametros (CB, Kafka, JWT)
        'http://localhost:18090',                                         # Kafka UI: topics y consumer groups
        'http://localhost:18180/realms/lamb/protocol/openid-connect/certs', # S7: clave publica (JWKS de Keycloak)
        'http://localhost:8083/actuator/circuitbreakers',                 # S6: estado del circuit breaker
        'http://localhost:8083/actuator/health',                          # S6: health con circuitBreakers
        'http://localhost:8083/swagger-ui.html',                          # planificacion
        'http://localhost:8088/swagger-ui.html',                          # curriculo
        'http://localhost:8086/swagger-ui.html',                          # evaluacion
        'http://localhost:8084/swagger-ui.html',                          # admision
        'http://localhost:8089/swagger-ui.html'                           # trayectoria
    )
    foreach ($p in $paginas) { Start-Process $p; Start-Sleep -Milliseconds 400 }
}

Write-Host ''
Write-Host 'Listo. Pruebas guiadas:  . .\scripts\demo-pruebas.ps1   y luego  Demo-Ayuda' -ForegroundColor Green
