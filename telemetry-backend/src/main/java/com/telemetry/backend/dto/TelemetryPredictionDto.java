package com.telemetry.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Collections;
import java.util.List;

/**
 * Deserializes BOTH possible response shapes from
 * POST /telemetry/{vehicleId} on the FastAPI ML service:
 *
 *  - status == "COLLECTING"          -> readingsReceived/readingsRequired set,
 *                                        prediction fields are null.
 *  - status == "ANOMALY" | "NORMAL"  -> full prediction, readings_* are null.
 *
 * Using one class for both keeps the Kafka consumer simple (no need to
 * introspect the raw JSON before deciding which class to deserialize into).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) // FastAPI sends snake_case (risk_level, root_causes, ...)
public class TelemetryPredictionDto {

    private String status; // "COLLECTING" | "ANOMALY" | "NORMAL"

    // Present only when status == COLLECTING
    private Integer readingsReceived;
    private Integer readingsRequired;
    private String vehicleId;

    // Present only when status == ANOMALY | NORMAL
    private Double probability;
    private String riskLevel;
    private Integer label;
    private String modelUsed;
    private List<RootCauseDto> rootCauses = Collections.emptyList();
    private String recommendation;

    public boolean isCollecting() {
        return "COLLECTING".equals(status);
    }

    public boolean isAnomaly() {
        return "ANOMALY".equals(status);
    }

    // --- getters / setters ---

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getReadingsReceived() { return readingsReceived; }
    public void setReadingsReceived(Integer readingsReceived) { this.readingsReceived = readingsReceived; }

    public Integer getReadingsRequired() { return readingsRequired; }
    public void setReadingsRequired(Integer readingsRequired) { this.readingsRequired = readingsRequired; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public Double getProbability() { return probability; }
    public void setProbability(Double probability) { this.probability = probability; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Integer getLabel() { return label; }
    public void setLabel(Integer label) { this.label = label; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

    public List<RootCauseDto> getRootCauses() { return rootCauses; }
    public void setRootCauses(List<RootCauseDto> rootCauses) { this.rootCauses = rootCauses; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}
