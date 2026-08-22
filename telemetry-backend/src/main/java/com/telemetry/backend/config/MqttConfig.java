package com.telemetry.backend.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
/**
 * MQTT side of the pipeline.
 *
 * Mosquitto is already configured with:
 *   listener 1883 (switch to 8883 + certs for TLS once exposed publicly)
 *   password_file passFile
 *   allow_anonymous false
 *
 * So this client MUST authenticate with a username/password that exists in
 * that passFile (create one for Spring specifically, e.g.:
 *   mosquitto_passwd -b passFile spring-backend-client <password>
 * ), matching mqtt.username / mqtt.password below.
 */
@Configuration
public class MqttConfig {
    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.topic-filter}")
    private String topicFilter;

    @Value("${mqtt.qos}")
    private int qos;

    @Value("${mqtt.ssl.truststore}")
    private String trustStorePath;

    @Value("${mqtt.ssl.truststore-password}")
    private String trustStorePassword;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() throws Exception {

        DefaultMqttPahoClientFactory factory =
                new DefaultMqttPahoClientFactory();

        MqttConnectOptions options = new MqttConnectOptions();

        options.setServerURIs(new String[]{brokerUrl});

        options.setUserName(username);
        options.setPassword(password.toCharArray());

        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        // TLS
        options.setSocketFactory(createSslSocketFactory());

        factory.setConnectionOptions(options);

        return factory;
    }

    private javax.net.ssl.SSLSocketFactory createSslSocketFactory()
            throws Exception {

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

        String trustStoreLocation = trustStorePath;

        if (trustStoreLocation.startsWith("classpath:")) {
            trustStoreLocation = trustStoreLocation.substring("classpath:".length());
        }

        ClassPathResource resource =
                new ClassPathResource(trustStoreLocation);

        try (InputStream inputStream = resource.getInputStream()) {

            trustStore.load(
                    inputStream,
                    trustStorePassword.toCharArray()
            );
        }

        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm()
                );

        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");

        sslContext.init(
                null,
                trustManagerFactory.getTrustManagers(),
                null
        );

        return sslContext.getSocketFactory();
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer mqttInbound() {

        MqttPahoMessageDrivenChannelAdapter adapter =
                null;
        try {
            adapter = new MqttPahoMessageDrivenChannelAdapter(
                    clientId,
                    mqttClientFactory(),
                    topicFilter
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        adapter.setCompletionTimeout(5000);
        adapter.setQos(qos);
        adapter.setOutputChannel(mqttInputChannel());

        return adapter;
    }
}
