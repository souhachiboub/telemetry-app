# telemetry-backend

Spring Boot service that bridges Mosquitto (MQTT) → Kafka → FastAPI ML API → Angular (WebSocket + REST).

## Pipeline

```
Device --MQTT--> Mosquitto --> [MqttConfig + MqttToKafkaBridge] --> Kafka topic "vehicle-telemetry"
                                                                          |
                                                                          v
                                                          [TelemetryConsumer @KafkaListener]
                                                                          |
                                                     POST /telemetry/{vehicleId} to FastAPI ML API
                                                                          |
                                                     save prediction (JPA) + push via WebSocket
                                                                          |
                                                                          v
                                                              Angular (/topic/telemetry/{id})
```

## Configure

Everything is externalized as environment variables (see `application.yml` for defaults):

| Variable | Purpose |
|---|---|
| `MQTT_BROKER_URL` | e.g. `ssl://mqtt.yourdomain.com:8883` |
| `MQTT_USERNAME` / `MQTT_PASSWORD` | must exist in Mosquitto's `passFile` (`mosquitto_passwd -b passFile spring-backend-client <pwd>`) |
| `KAFKA_BOOTSTRAP_SERVERS` | e.g. `kafka:9092` |
| `ML_API_URL` | base URL of the FastAPI service (`main.py`), e.g. `https://ml-api.yourdomain.com` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Postgres connection (defaults to in-memory H2 for local dev) |

Mosquitto side (already done per your setup): `listener 1883` (switch to `8883` + TLS certs once exposed publicly), `password_file passFile`, `allow_anonymous false`. The device topic pattern expected here is `devices/{vehicleId}/telemetry` — adjust `mqtt.topic-filter` if yours differs.

## Run locally

```bash
mvn spring-boot:run
```

Requires a reachable Mosquitto, Kafka, and the FastAPI ML service (see the `main.py` / `README.md` you already have) running first — the app fails fast if Kafka or MQTT aren't reachable at startup.

## Endpoints exposed to Angular

- `ws(s)://<host>/ws` — STOMP WebSocket. Subscribe to `/topic/telemetry/{vehicleId}` for one vehicle, or `/topic/telemetry` for all vehicles.
- `GET /api/telemetry/{vehicleId}/history` — last 50 predictions for a vehicle (for charts).
- `GET /api/telemetry/devices` — latest known state per vehicle (for the Live dashboard table).
- `GET /api/telemetry/anomalies` — 20 most recent anomalies across all vehicles.

## Known limitations / next steps

- `TelemetryPredictionRepository.findLatestPerVehicle()` uses Postgres' `DISTINCT ON`, which won't run against the default H2 dev profile — point `DB_URL` at a real Postgres instance, or rewrite it for local testing.
- CORS is wide open (`*`) — restrict `@CrossOrigin` and the WebSocket `setAllowedOriginPatterns` to your actual Angular origin before going to production.
- The ML API call in `TelemetryConsumer` is fire-and-forget reactive (`.subscribe(...)`) — for production you likely want retry/backoff (e.g. `WebClient`'s `.retryWhen(...)`) in case the ML API is briefly unavailable.
