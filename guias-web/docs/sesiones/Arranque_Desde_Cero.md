# Arranque desde cero (solo comandos)

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Para cuando ya terminaste la guía - 2026*

Todo el proyecto, con tu servicio incluido, en orden. Abre PowerShell en la raíz del proyecto
(`Lamb-Modulo02-Admision-Trayectoria`). Cambia `lamb-evaluacionn-ms` por tu servicio.

## Paso 0. Solo la primera vez

```powershell
cd clients\lamb-frontend
npm install
cd ..\..
```

## Paso 1. Docker (abre Docker Desktop primero)

Una sola terminal, pega todo:

```powershell
docker compose -f kafka/compose-dev.yml up -d
docker compose -f keycloak/compose-dev.yml up -d
docker compose -f services/lamb-admision-ms/compose-dev.yml up -d
docker compose -f services/lamb-trayectoria-ms/compose-dev.yml up -d
docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d
docker compose -f obs/compose-dev.yml up -d
```

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}"
```

## Paso 2. Config Server (terminal 1)

```powershell
cd infra\lamb-config
.\mvnw.cmd spring-boot:run
```

Espera `Started` → http://localhost:18888/lamb-evaluacionn-ms/dev

## Paso 3. Eureka (terminal 2)

```powershell
cd infra\lamb-eureka
.\mvnw.cmd spring-boot:run
```

Espera `Started` → http://localhost:18761

## Paso 4. Admisión (terminal 3)

```powershell
cd services\lamb-admision-ms
.\mvnw.cmd spring-boot:run
```

## Paso 5. Trayectoria (terminal 4)

```powershell
cd services\lamb-trayectoria-ms
.\mvnw.cmd spring-boot:run
```

## Paso 6. Tu servicio (terminal 5)

```powershell
cd services\lamb-evaluacionn-ms
.\mvnw.cmd spring-boot:run
```

Espera `Started` → http://localhost:8095/actuator/health

## Paso 7. Gateway (terminal 6)

Cuando los servicios ya salgan en http://localhost:18761 (~30 s):

```powershell
cd infra\lamb-gateway
.\mvnw.cmd spring-boot:run
```

## Paso 8. Frontend (terminal 7)

```powershell
cd clients\lamb-frontend
npm start
```

## Paso 9. Comprobar

```powershell
Invoke-RestMethod http://localhost:18080/actuator/health
Invoke-RestMethod http://localhost:18080/api/v1/evaluacionn/saludo
Invoke-RestMethod http://localhost:18080/api/v1/examenes          # solo camino B (con CRUD)
```

| Abrir | Link |
|---|---|
| Frontend | http://localhost:4200 |
| Tu servicio | http://localhost:4200/evaluacionn |
| Eureka | http://localhost:18761 |
| Kafka UI | http://localhost:18090 |
| Keycloak | http://localhost:18180 |
| Grafana | http://localhost:13000 |
| Swagger | http://localhost:8095/swagger-ui.html |

## Parar todo

```powershell
foreach ($p in 4200, 18080, 8095, 8089, 8084, 18761, 18888) {
    $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($c) { Stop-Process -Id $c.OwningProcess -Force; "Detenido puerto $p" }
}
docker compose -f obs/compose-dev.yml stop
docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml stop
docker compose -f services/lamb-trayectoria-ms/compose-dev.yml stop
docker compose -f services/lamb-admision-ms/compose-dev.yml stop
docker compose -f keycloak/compose-dev.yml stop
docker compose -f kafka/compose-dev.yml stop
```

Si algo falla: [Solución de errores](Solucion_Errores.md).
