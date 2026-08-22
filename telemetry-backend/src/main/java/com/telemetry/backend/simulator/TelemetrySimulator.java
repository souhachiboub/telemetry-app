package com.telemetry.backend.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Builds JSON payloads matching main.py's TelemetryRecord schema exactly
 * (Trip_Time_s, Speed_kmh, Motor_RPM, ...). No vehicle_id field is included
 * in the payload -- the vehicle id is carried by the MQTT topic
 * (devices/{vehicleId}/telemetry), same as a real device would do.
 *
 * Two scenarios:
 *  - "normal": small random jitter around cruising values, stays in range
 *    forever -- use this to check the pipeline reports NORMAL/LOW risk.
 *  - "anomaly": progressively ramps speed/RPM/temperature/current up each
 *    reading (mirrors the escalating pattern in your own test.json), so
 *    after ~window_size readings the model should flag ANOMALY / HIGH risk
 *    with root causes on Battery_Current_A and Speed_kmh.
 */
@Component
public class TelemetrySimulator {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public String buildNormalReading(double tripTimeS) {
        Map<String, Object> row = baseRow(tripTimeS);
        row.put("Speed_kmh", jitter(85, 5));
        row.put("Motor_RPM", jitter(6000, 300));
        row.put("Motor_Temp_C", jitter(65, 3));
        row.put("Battery_SOC_pct", jitter(55, 2));
        row.put("Battery_Voltage_V", jitter(355, 2));
        row.put("Battery_Current_A", jitter(30, 4));
        row.put("Battery_Temp_C", jitter(32, 2));
        row.put("Braking_Active", "OFF");
        row.put("Vehicle_State", "Cruising");
        return toJson(row);
    }

    /**
     * step goes from 0 (first reading) upward; the further along, the more
     * extreme the values -- reproduces the escalating profile from your
     * test.json so you can watch risk_level climb from LOW to HIGH live on
     * the dashboard as successive readings are published.
     */
    public String buildAnomalyReading(double tripTimeS, int step) {
        Map<String, Object> row = baseRow(tripTimeS);
        row.put("Speed_kmh", 85 + step * 5);
        row.put("Motor_RPM", 6000 + step * 400);
        row.put("Motor_Temp_C", 65 + step * 4);
        row.put("Battery_SOC_pct", Math.max(40, 55 - step * 0.5));
        row.put("Battery_Voltage_V", 355 - step * 0.4);
        row.put("Battery_Current_A", 30 + step * 4);
        row.put("Battery_Temp_C", 32 + step * 2);
        row.put("Braking_Active", "OFF");
        row.put("Vehicle_State", "Accelerating");
        return toJson(row);
    }

    private Map<String, Object> baseRow(double tripTimeS) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Trip_Time_s", tripTimeS);
        row.put("Ambient_Temp_C", 24.0);
        return row;
    }

    private double jitter(double base, double spread) {
        return Math.round((base + (random.nextDouble() - 0.5) * 2 * spread) * 10) / 10.0;
    }

    private String toJson(Map<String, Object> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize simulated reading", e);
        }
    }
}
