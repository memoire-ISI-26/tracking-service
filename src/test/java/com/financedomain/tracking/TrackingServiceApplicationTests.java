package com.financedomain.tracking;

import com.financedomain.tracking.repository.TrackingEventRepository;
import com.financedomain.tracking.service.TrackingService;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "server.port=8203",
        "tracking-service.uriport=8203",
        "tracking-service.urlregistry=http://localhost:8761/eureka",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.kafka.admin.auto-create=false"
})
class TrackingServiceApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private TrackingEventRepository trackingEventRepository;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private NewTopic trackingEventsTopic;

    @Autowired
    private TrackingService trackingService;

    @Test
    @DisplayName("Vérifie le chargement du contexte Spring Boot et des beans pour tracking-service")
    void contextLoads() {
        assertNotNull(applicationContext, "Le contexte Spring Boot du tracking-service doit être correctement initialisé.");
        assertThat(trackingService).isNotNull();
    }
}
