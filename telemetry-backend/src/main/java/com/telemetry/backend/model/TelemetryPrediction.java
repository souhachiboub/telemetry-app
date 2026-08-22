package com.telemetry.backend.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "telemetry_predictions", indexes = {
        @Index(name = "idx_vehicle_created", columnList = "vehicleId,createdAt")
})
public class TelemetryPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String vehicleId;

    @Column(nullable = false)
    private String status; // ANOMALY | NORMAL

    private double probability;

    private String riskLevel; // LOW | MEDIUM | HIGH

    private int label;

    private String modelUsed;

    @Column(length = 1000)
    private String recommendation;

    @ElementCollection
    @CollectionTable(name = "telemetry_root_causes", joinColumns = @JoinColumn(name = "prediction_id"))
    private List<RootCauseEmbeddable> rootCauses = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public TelemetryPrediction() {}

    // --- getters / setters ---

    public Long getId() { return id; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getProbability() { return probability; }
    public void setProbability(double probability) { this.probability = probability; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public int getLabel() { return label; }
    public void setLabel(int label) { this.label = label; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public List<RootCauseEmbeddable> getRootCauses() { return rootCauses; }
    public void setRootCauses(List<RootCauseEmbeddable> rootCauses) { this.rootCauses = rootCauses; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
