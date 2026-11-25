# Ejecución del proyecto — Módulo 02 Admisión-Trayectoria

Ejecuta cada comando en su propia terminal, **en el orden indicado**.

## 1–4. Infra Docker

```powershell
docker compose -f kafka/compose-dev.yml up -d
docker compose -f keycloak/compose-dev.yml up -d
docker compose -f services/lamb-admision-ms/compose-dev.yml up -d
docker compose -f services/lamb-trayectoria-ms/compose-dev.yml up -d
```

## 5. Config Server (espera a que arranque antes del siguiente)

```powershell
cd infra\lamb-config
.\mvnw.cmd spring-boot:run
```

## 6. Eureka (espera a que arranque)

```powershell
cd infra\lamb-eureka
.\mvnw.cmd spring-boot:run
```

## 7. Admisión

```powershell
cd services\lamb-admision-ms
.\mvnw.cmd spring-boot:run
```

## 8. Trayectoria

```powershell
cd services\lamb-trayectoria-ms
.\mvnw.cmd spring-boot:run
```

## 9. Gateway

```powershell
cd infra\lamb-gateway
.\mvnw.cmd spring-boot:run
```

## 10. Frontend

```powershell
cd clients\lamb-frontend
npm install
npm start
```

Abrir: http://localhost:4200

---

## URLs útiles

| Servicio         | URL                              |
|------------------|----------------------------------|
| Frontend         | http://localhost:4200            |
| Eureka           | http://localhost:18761           |
| Gateway          | http://localhost:18080           |
| Kafka UI         | http://localhost:18090           |
| Keycloak         | http://localhost:18180           |
| Config Server    | http://localhost:18888           |
| Admisión (ms)    | http://localhost:8084            |
| Trayectoria (ms) | http://localhost:8089            |

---

## Parar todo

```powershell
powershell -ExecutionPolicy Bypass -File scripts\demo-stop.ps1
docker compose -f kafka/compose-dev.yml down
docker compose -f keycloak/compose-dev.yml down
docker compose -f services/lamb-admision-ms/compose-dev.yml down
docker compose -f services/lamb-trayectoria-ms/compose-dev.yml down
```
