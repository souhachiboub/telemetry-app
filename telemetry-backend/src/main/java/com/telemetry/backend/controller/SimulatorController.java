package com.telemetry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetry.backend.config.MqttOutboundConfig;
import com.telemetry.backend.dto.ManualTelemetryRequest;
import com.telemetry.backend.simulator.TelemetrySimulator;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Powers the "Test publisher" page of the dashboard: publishes fake but
 * realistic telemetry over the SAME MQTT topic a real device would use, so
 * it exercises the entire pipeline (MQTT -> Kafka -> ML API -> WebSocket)
 * end-to-end, with no physical device required.
 *
 * This is a demo/testing tool, not a production endpoint -- guard it (e.g.
 * behind a "dev"/"demo" Spring profile, or basic auth) before deploying
 * anywhere it could be reached publicly.
 */

@RestController
@RequestMapping("/api/simulator")
@CrossOrigin(origins = "${app.frontend-url}")
public class SimulatorController {

    private final MqttOutboundConfig.MqttGateway mqttGateway;
    private final TelemetrySimulator simulator;
    private final ObjectMapper objectMapper;
    public SimulatorController(MqttOutboundConfig.MqttGateway mqttGateway,
                                TelemetrySimulator simulator) {
        this.mqttGateway = mqttGateway;
        this.simulator = simulator;
        this.objectMapper=new ObjectMapper();
    }

    /**
     * Publishes ONE reading for a vehicle. Handy to nudge the "collecting
     * X/15" counter forward one step at a time from the UI.
     *
     * scenario = "normal" | "anomaly"
     * step     = only used for "anomaly", how far along the escalation to publish
     */
    @PostMapping("/publish/{vehicleId}")
    public Map<String, Object> publishOne(@PathVariable String vehicleId,
                                           @RequestParam(defaultValue = "normal") String scenario,
                                           @RequestParam(defaultValue = "0") int step) {
        String payload = "anomaly".equalsIgnoreCase(scenario)
                ? simulator.buildAnomalyReading(500 + step, step)
                : simulator.buildNormalReading(500 + step);

        mqttGateway.publish("devices/" + vehicleId + "/telemetry", payload);

        return Map.of(
                "published", true,
                "vehicleId", vehicleId,
                "topic", "devices/" + vehicleId + "/telemetry",
                "scenario", scenario,
                "payload", payload
        );
    }

    /**
     * Publishes `count` consecutive readings in one call -- e.g. count=15 to
     * immediately fill the ML API's sliding window and get a real prediction
     * back over the WebSocket, without clicking 15 times.
     */
    @PostMapping("/publish/{vehicleId}/batch")
    public Map<String, Object> publishBatch(@PathVariable String vehicleId,
                                             @RequestParam(defaultValue = "normal") String scenario,
                                             @RequestParam(defaultValue = "15") int count) {
        for (int step = 0; step < count; step++) {
            String payload = "anomaly".equalsIgnoreCase(scenario)
                    ? simulator.buildAnomalyReading(500 + step, step)
                    : simulator.buildNormalReading(500 + step);
            mqttGateway.publish("devices/" + vehicleId + "/telemetry", payload);
        }

        return Map.of(
                "published", true,
                "vehicleId", vehicleId,
                "scenario", scenario,
                "count", count,
                "hint", "Subscribe to /topic/telemetry/" + vehicleId + " to watch the predictions arrive live."
        );
    }

    @PostMapping("/publish-manual")
    public ResponseEntity<?> publishManual(
            @RequestBody ManualTelemetryRequest request) {

        if (request.getVehicleId() == null ||
                request.getVehicleId().isBlank()) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "error", "vehicleId is required"
                    )
            );
        }

        if (request.getTelemetry() == null) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "error", "telemetry is required"
                    )
            );
        }

        try {

            String payload =
                    objectMapper.writeValueAsString(
                            request.getTelemetry()
                    );

            String topic =
                    "devices/" +
                            request.getVehicleId() +
                            "/telemetry";

            mqttGateway.publish(topic, payload);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "published", true,
                            "vehicleId", request.getVehicleId(),
                            "topic", topic,
                            "payload", request.getTelemetry()
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "success", false,
                            "error", e.getMessage()
                    )
            );
        }
    }
}
