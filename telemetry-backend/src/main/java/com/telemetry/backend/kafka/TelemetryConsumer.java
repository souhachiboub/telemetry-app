package com.telemetry.backend.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetry.backend.dto.RootCauseDto;
import com.telemetry.backend.dto.TelemetryPredictionDto;
import com.telemetry.backend.model.RootCauseEmbeddable;
import com.telemetry.backend.model.TelemetryPrediction;
import com.telemetry.backend.repository.TelemetryPredictionRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consumes each raw telemetry JSON message from Kafka, forwards it to the
 * FastAPI ML service's stateful endpoint (POST /telemetry/{vehicleId}), then:
 *
 *   - status == COLLECTING -> just push the progress to the UI, nothing to persist.
 *   - status == ANOMALY|NORMAL -> persist the prediction and push it to the UI.
 *
 * The Kafka message key is the vehicleId (set by MqttToKafkaBridge), so all
 * of one vehicle's readings are consumed in order.
 */
@Component

public class TelemetryConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TelemetryConsumer.class);

    private final WebClient mlApiClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final TelemetryPredictionRepository repository;
    private final ObjectMapper objectMapper;

    private final long mlTimeoutMs;

    public TelemetryConsumer(
            WebClient mlApiClient,
            SimpMessagingTemplate messagingTemplate,
            TelemetryPredictionRepository repository,
            ObjectMapper objectMapper,
            @Value("${ml.api.timeout-ms:5000}") long mlTimeoutMs) {

        this.mlApiClient = mlApiClient;
        this.messagingTemplate = messagingTemplate;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.mlTimeoutMs = mlTimeoutMs;
    }

    @KafkaListener(
            topics = "${kafka.topic.telemetry-raw}",
            groupId = "telemetry-consumer"
    )
    public void consume(ConsumerRecord<String, String> record) {

        String vehicleId = record.key();
        String rawJson = record.value();

        if (vehicleId == null || vehicleId.isBlank()) {

            log.warn(
                    "Missing vehicleId in Kafka key. Payload={}",
                    rawJson
            );

            return;
        }

        try {

            log.debug(
                    "Processing telemetry for vehicle {}",
                    vehicleId
            );

            TelemetryPredictionDto prediction =
                    mlApiClient.post()
                            .uri(
                                    "/telemetry/{vehicleId}",
                                    vehicleId
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .bodyValue(rawJson)
                            .retrieve()
                            .bodyToMono(
                                    TelemetryPredictionDto.class
                            )
                            .block(
                                    Duration.ofMillis(
                                            mlTimeoutMs
                                    )
                            );

            if (prediction == null) {

                throw new IllegalStateException(
                        "ML API returned an empty response"
                );
            }

            handlePrediction(
                    vehicleId,
                    prediction
            );

        } catch (Exception e) {

            log.error(
                    "ML processing failed for vehicle {}",
                    vehicleId,
                    e
            );

            pushError(
                    vehicleId,
                    e
            );

            /*
             * Important:
             * allow Kafka error handling/retry mechanisms
             * to detect the failure.
             */
            throw new RuntimeException(
                    "ML processing failed for vehicle "
                            + vehicleId,
                    e
            );
        }
    }

    private void handlePrediction(
            String vehicleId,
            TelemetryPredictionDto prediction) {

        /*
         * ML is still collecting the sliding window.
         */
        if (prediction.isCollecting()) {

            messagingTemplate.convertAndSend(
                    "/topic/telemetry/" + vehicleId,
                    prediction
            );

            messagingTemplate.convertAndSend(
                    "/topic/telemetry",
                    prediction
            );

            return;
        }

        /*
         * We have a complete ML prediction.
         */
        TelemetryPrediction entity =
                toEntity(
                        vehicleId,
                        prediction
                );

        repository.save(entity);

        if (prediction.isAnomaly()) {

            log.warn(
                    "ANOMALY detected for vehicle {} " +
                            "(risk={}, probability={})",
                    vehicleId,
                    prediction.getRiskLevel(),
                    prediction.getProbability()
            );
        }

        /*
         * Vehicle-specific dashboard.
         */
        messagingTemplate.convertAndSend(
                "/topic/telemetry/" + vehicleId,
                prediction
        );

        /*
         * Global dashboard.
         */
        messagingTemplate.convertAndSend(
                "/topic/telemetry",
                prediction
        );
    }

    private void pushError(
            String vehicleId,
            Throwable err) {

        messagingTemplate.convertAndSend(
                "/topic/telemetry/" + vehicleId,
                Map.of(
                        "status", "ERROR",
                        "vehicleId", vehicleId,
                        "message",
                        err.getMessage() != null
                                ? err.getMessage()
                                : "Unknown error",
                        "errorType",
                        err.getClass().getSimpleName(),
                        "timestamp",
                        Instant.now().toString()
                )
        );
    }

    private TelemetryPrediction toEntity(
            String vehicleId,
            TelemetryPredictionDto dto) {

        TelemetryPrediction entity =
                new TelemetryPrediction();

        entity.setVehicleId(vehicleId);

        entity.setStatus(
                dto.getStatus()
        );

        entity.setProbability(
                dto.getProbability() != null
                        ? dto.getProbability()
                        : 0.0
        );

        entity.setRiskLevel(
                dto.getRiskLevel()
        );

        entity.setLabel(
                dto.getLabel() != null
                        ? dto.getLabel()
                        : 0
        );

        entity.setModelUsed(
                dto.getModelUsed()
        );

        entity.setRecommendation(
                dto.getRecommendation()
        );

        entity.setCreatedAt(
                Instant.now()
        );

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
