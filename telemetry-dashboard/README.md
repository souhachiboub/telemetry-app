# telemetry-dashboard (Angular)

Frontend for the vehicle telemetry pipeline: MQTT → Kafka → FastAPI ML → **Spring Boot (`telemetry-backend`, port 8084)** → this app.

## Run locally

```bash
npm install
npm start
```

Opens on `http://localhost:4200`. `proxy.conf.json` forwards `/api/**` and `/ws` to `http://localhost:8084` (your Spring Boot backend), so no CORS setup is needed in dev — the browser only ever talks to `localhost:4200`.

In production, either serve this build behind the same reverse proxy as the backend (same pattern: `/api` and `/ws` → Spring Boot), or point `app.frontend-url` in the backend's `application.properties` at wherever this app is actually hosted, since `TelemetryController`, `SimulatorController`, and `WebSocketConfig` all read CORS origins from that property.

## Pages ↔ backend endpoints

| Page | Route | Backend source |
|---|---|---|
| Live dashboard | `/live` | `GET /api/telemetry/devices` (initial) + `/topic/telemetry` (WebSocket, live) |
| Devices | `/devices` | `GET /api/telemetry/devices` |
| Device detail / Analytics | `/devices/:vehicleId`, `/analytics` | `GET /api/telemetry/{vehicleId}/history`, `GET /api/telemetry/anomalies` |
| Alerts | `/alerts` | `GET /api/telemetry/anomalies` (initial) + `/topic/telemetry` filtered to `ANOMALY` (live) |
| Test publisher | `/simulator` | `POST /api/simulator/publish/{vehicleId}`, `/publish/{vehicleId}/batch`, `/publish-manual` + `/topic/telemetry/{vehicleId}` (watches the result live) |

## Key files

- `src/app/services/telemetry-websocket.service.ts` — single shared STOMP/SockJS connection; components subscribe to `ws.global$`, `ws.forVehicle(id)`, or `ws.anomalies$()` rather than opening their own socket.
- `src/app/models/telemetry.model.ts` — TypeScript interfaces mirroring the backend DTOs field-for-field (including the snake_case vs camelCase split between `TelemetryPredictionDto`/WebSocket payloads and the persisted `TelemetryPrediction` entity/REST responses).
- `src/app/pages/simulator/` — the "Test publisher" page; only page that should be gated/removed before a real production deployment, since `SimulatorController` is a demo-only endpoint.

## Not yet wired

- Auth — none of the REST/WebSocket calls carry a token; add an `HttpInterceptor` and STOMP `connectHeaders` once the backend has real auth.
- The `Devices` page and `Live dashboard` currently show a flat list — if `findLatestPerVehicle()` scales past a few dozen vehicles, add pagination/search here.
