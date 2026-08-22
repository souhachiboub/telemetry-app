package com.telemetry.backend.repository;

import com.telemetry.backend.model.TelemetryPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelemetryPredictionRepository extends JpaRepository<TelemetryPrediction, Long> {

    List<TelemetryPrediction> findTop50ByVehicleIdOrderByCreatedAtDesc(String vehicleId);

    List<TelemetryPrediction> findTop20ByStatusOrderByCreatedAtDesc(String status);

    // One row per vehicle, most recent first -- used for the "Live dashboard"
    // devices table. DISTINCT ON is Postgres-specific (your prod DB); it will
    // NOT run against the default H2 dev profile. For local H2 testing, either
    // switch DB_URL to a real Postgres instance or temporarily replace this
    // with fetching all rows and grouping in Java.
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT DISTINCT ON (vehicle_id) *
            FROM telemetry_predictions
            ORDER BY vehicle_id, created_at DESC
            """, nativeQuery = true)
    List<TelemetryPrediction> findLatestPerVehicle();
}
