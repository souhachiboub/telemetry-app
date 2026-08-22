package com.telemetry.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;

/**
 * Outbound side of MQTT: lets the backend itself PUBLISH messages, reusing
 * the same broker connection factory as MqttConfig (inbound). This is what
 * powers the telemetry simulator (see SimulatorController) -- it publishes
 * on devices/{vehicleId}/telemetry exactly like a real device would, so it
 * exercises the FULL pipeline (MQTT -> Kafka -> ML API -> WebSocket),
 * not just the ML call in isolation.
 */
@Configuration
public class MqttOutboundConfig {

    @Value("${mqtt.client-id}")
    private String clientId;

    private final MqttPahoClientFactory mqttPahoClientFactory;

    public MqttOutboundConfig(MqttPahoClientFactory mqttPahoClientFactory) {
        this.mqttPahoClientFactory = mqttPahoClientFactory;
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler handler =
                new MqttPahoMessageHandler(clientId + "-publisher", mqttPahoClientFactory);
        handler.setAsync(true);
        handler.setDefaultQos(1);
        return handler;
    }

    /**
     * Simple typed gateway: call publish(topic, payload) from Java code and
     * Spring Integration handles turning it into an outbound MQTT message.
     */
    @MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
    public interface MqttGateway {

        void publish(
                @Header("mqtt_topic") String topic,
                String payload
        );
    }
}
