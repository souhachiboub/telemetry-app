package com.telemetry.backend.controller;

import com.telemetry.backend.dto.MlPredictionResponse;
import com.telemetry.backend.dto.VehicleTelemetryReading;
import com.telemetry.backend.model.TelemetryPrediction;
import com.telemetry.backend.repository.TelemetryPredictionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Complements the WebSocket push (live data) with plain REST endpoints for
 * the historical / list views of the dashboard (Devices page, Analytics
 * charts, initial page load before the WebSocket connects).
 */
@RestController
@RequestMapping("/api/telemetry")
@CrossOrigin(origins = "${app.frontend-url}")
public class TelemetryController {
    private final WebClient mlWebClient;
    private final TelemetryPredictionRepository repository;

    public TelemetryController(WebClient mlWebClient, TelemetryPredictionRepository repository) {
        this.mlWebClient = mlWebClient;
        this.repository = repository;
    }

    /** Last 50 predictions for one vehicle, most recent first -- feeds the anomaly-score chart. */
    @GetMapping("/{vehicleId}/history")
    public List<TelemetryPrediction> history(@PathVariable String vehicleId) {
        return repository.findTop50ByVehicleIdOrderByCreatedAtDesc(vehicleId);
    }

    /** Latest known state per vehicle -- feeds the "Live dashboard" devices table. */
    @GetMapping("/devices")
    public List<TelemetryPrediction> devices() {
        return repository.findLatestPerVehicle();
    }

    /** Most recent anomalies across all vehicles -- feeds "Recent ML detections". */
    @GetMapping("/anomalies")
    public List<TelemetryPrediction> recentAnomalies() {
        return repository.findTop20ByStatusOrderByCreatedAtDesc("ANOMALY");
    }

    @PostMapping("/predict/manual")
    public ResponseEntity<?> predictManual(
            @RequestBody VehicleTelemetryReading reading) {

        try {
            MlPredictionResponse resp = mlWebClient.post()
                    .uri("/predict/single")
                    .bodyValue(reading)
                    .retrieve()
                    .bodyToMono(MlPredictionResponse.class)
                    .block();

            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            return ResponseEntity.status(502)
                    .body("FastAPI call failed: " + ex.getMessage());
        }
    }

}
