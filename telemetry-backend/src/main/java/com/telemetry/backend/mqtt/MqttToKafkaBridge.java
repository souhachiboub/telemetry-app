package com.telemetry.backend.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Every JSON telemetry payload the device publishes on
 * "devices/{deviceId}/telemetry" lands here and is forwarded, unmodified,
 * onto a Kafka topic. Kafka is what actually buffers/decouples ingestion
 * from processing -- MQTT itself doesn't retain messages once delivered.
 */
@Component
public class MqttToKafkaBridge {

    private static final Logger log = LoggerFactory.getLogger(MqttToKafkaBridge.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String telemetryTopic;

    public MqttToKafkaBridge(KafkaTemplate<String, String> kafkaTemplate,
                              @Value("${kafka.topic.telemetry-raw}") String telemetryTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.telemetryTopic = telemetryTopic;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttMessage(Message<String> message,
                                   @Header(MqttHeaders.RECEIVED_TOPIC) String mqttTopic) {
        String payload = message.getPayload();

        // mqttTopic looks like "devices/device-014/telemetry" -> extract the
        // device/vehicle id so it can be used as the Kafka partition key.
        // Keying by vehicle id keeps all of one vehicle's messages in the
        // same partition, in order -- important since the ML API buffers a
        // sliding window per vehicle and expects readings in sequence.
        String vehicleId = extractVehicleId(mqttTopic);

        log.debug("MQTT -> Kafka [{}] topic={} payload={}", vehicleId, mqttTopic, payload);
        kafkaTemplate.send(telemetryTopic, vehicleId, payload);
    }

    private String extractVehicleId(String mqttTopic) {
        String[] parts = mqttTopic.split("/");
        return parts.length >= 2 ? parts[1] : "unknown";
    }
}
