package com.telemetry.backend.service;

import com.telemetry.backend.dto.RootCauseDto;
import com.telemetry.backend.dto.TelemetryPredictionDto;
import com.telemetry.backend.model.RootCauseEmbeddable;
import com.telemetry.backend.model.TelemetryPrediction;
import com.telemetry.backend.repository.TelemetryPredictionRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TelemetryProcessingService {



    private final WebClient mlApiClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final TelemetryPredictionRepository repository;

    public TelemetryProcessingService(
            WebClient mlApiClient,
            SimpMessagingTemplate messagingTemplate,
            TelemetryPredictionRepository repository) {

        this.mlApiClient = mlApiClient;
        this.messagingTemplate = messagingTemplate;
        this.repository = repository;
    }

    public void processTelemetry(
            String vehicleId,
            String telemetryJson) {

        log.info(
                "Sending telemetry to ML: vehicle={}, payload={}",
                vehicleId,
                telemetryJson
        );

        mlApiClient.post()
                .uri("/telemetry/{vehicleId}", vehicleId)
                .header("Content-Type", "application/json")
                .bodyValue(telemetryJson)
                .retrieve()
                .bodyToMono(TelemetryPredictionDto.class)
                .doOnNext(prediction ->
                        handlePrediction(vehicleId, prediction)
                )
                .doOnError(error ->
                        handleError(vehicleId, error)
                )
                .subscribe();
    }

    private void handlePrediction(
            String vehicleId,
            TelemetryPredictionDto prediction) {

        log.info(
                "ML prediction: vehicle={}, status={}, probability={}, risk={}",
                vehicleId,
                prediction.getStatus(),
                prediction.getProbability(),
                prediction.getRiskLevel()
        );

        /*
         * ML needs more readings before it can make a prediction.
         */
        if (prediction.isCollecting()) {

            messagingTemplate.convertAndSend(
                    "/topic/telemetry/" + vehicleId,
                    prediction
            );

            return;
        }

        /*
         * NORMAL or ANOMALY
         */
        TelemetryPrediction entity =
                toEntity(vehicleId, prediction);

        repository.save(entity);

        /*
         * Vehicle-specific WebSocket
         */
        messagingTemplate.convertAndSend(
                "/topic/telemetry/" + vehicleId,
                prediction
        );

        /*
         * Global dashboard WebSocket
         */
        messagingTemplate.convertAndSend(
                "/topic/telemetry",
                prediction
        );

        if (prediction.isAnomaly()) {

            log.warn(
                    "ANOMALY detected: vehicle={}, risk={}, probability={}",
                    vehicleId,
                    prediction.getRiskLevel(),
                    prediction.getProbability()
            );
        }
    }

    private void handleError(
            String vehicleId,
            Throwable error) {

        log.error(
                "ML API failed for vehicle {}: {}",
                vehicleId,
                error.getMessage()
        );

        messagingTemplate.convertAndSend(
                "/topic/telemetry/" + vehicleId,
                new java.util.HashMap<String, Object>() {{
                    put("status", "ERROR");
                    put("vehicleId", vehicleId);
                    put(
                            "message",
                            "ML API unreachable: " + error.getMessage()
                    );
                    put(
                            "timestamp",
                            Instant.now().toString()
                    );
                }}
        );
    }

    private TelemetryPrediction toEntity(
            String vehicleId,
            TelemetryPredictionDto dto) {

        TelemetryPrediction entity =
                new TelemetryPrediction();

        entity.setVehicleId(vehicleId);
        entity.setStatus(dto.getStatus());

        entity.setProbability(
                dto.getProbability() != null
                        ? dto.getProbability()
                        : 0.0
        );

        entity.setRiskLevel(dto.getRiskLevel());

        entity.setLabel(
                dto.getLabel() != null
                        ? dto.getLabel()
                        : 0
        );

        entity.setModelUsed(dto.getModelUsed());
        entity.setRecommendation(dto.getRecommendation());
        entity.setCreatedAt(Instant.now());

        List<RootCauseEmbeddable> causes =
                dto.getRootCauses() == null
                        ? List.of()
                        : dto.getRootCauses()
                        .stream()
                        .map(this::toEmbeddable)
                        .collect(Collectors.toList());

        entity.setRootCauses(causes);

        return entity;
    }

    private RootCauseEmbeddable toEmbeddable(
            RootCauseDto dto) {

        return new RootCauseEmbeddable(
                dto.getSensor(),
                dto.getValue(),
                dto.getThreshold(),
                dto.getIssue()
        );
    }
}