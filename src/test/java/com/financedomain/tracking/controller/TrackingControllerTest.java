package com.financedomain.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.financedomain.tracking.dto.TrackingEvent;
import com.financedomain.tracking.service.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrackingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TrackingService trackingService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TrackingController trackingController;

    private ObjectMapper objectMapper;

    private TrackingEvent sampleEvent;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(trackingController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleEvent = TrackingEvent.builder()
                .id("evt-001")
                .eventType("PURCHASE_PASS")
                .msisdn("771234567")
                .userId("1")
                .userRole("CLIENT")
                .sourceService("pricing-service")
                .payload(Map.of("passId", 1))
                .timestamp(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Devrait retourner 401 Unauthorized si l'en-tête X-User-Role est absent lors de la collecte d'un événement")
    void shouldRejectCollectEventWhenXUserRoleIsNull() throws Exception {
        mockMvc.perform(post("/tracking/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleEvent)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devrait collecter l'événement (201 Created) et publier vers Kafka quand le rôle est fourni")
    void shouldCollectEventAndPublishToKafkaWhenRoleIsProvided() throws Exception {
        when(trackingService.collect(any(TrackingEvent.class))).thenReturn(sampleEvent);

        mockMvc.perform(post("/tracking/event")
                        .header("X-User-Role", "CLIENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleEvent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("evt-001"))
                .andExpect(jsonPath("$.eventType").value("PURCHASE_PASS"));

        verify(trackingService).collect(any(TrackingEvent.class));
        verify(kafkaTemplate).send(eq("tracking-events"), eq("771234567"), any(TrackingEvent.class));
    }

    @Test
    @DisplayName("Devrait continuer (201 Created) même si l'envoi vers Kafka échoue")
    void shouldHandleKafkaErrorGracefullyWhenPublishFails() throws Exception {
        when(trackingService.collect(any(TrackingEvent.class))).thenReturn(sampleEvent);
        doThrow(new RuntimeException("Kafka unreachable")).when(kafkaTemplate).send(anyString(), anyString(), any());

        mockMvc.perform(post("/tracking/event")
                        .header("X-User-Role", "INTERNAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleEvent)))
                .andExpect(status().isCreated());

        verify(trackingService).collect(any(TrackingEvent.class));
    }

    @Test
    @DisplayName("Devrait retourner 401 Unauthorized pour getAllEvents sans rôle")
    void shouldRejectGetAllEventsWhenXUserRoleIsNull() throws Exception {
        mockMvc.perform(get("/tracking/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devrait retourner 403 Forbidden pour getAllEvents si le rôle n'est pas ADMINISTRATOR")
    void shouldRejectGetAllEventsWhenRoleIsNotAdministrator() throws Exception {
        mockMvc.perform(get("/tracking/events")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Devrait retourner 200 OK et tous les événements pour un ADMINISTRATOR")
    void shouldReturnAllEventsWhenRoleIsAdministrator() throws Exception {
        when(trackingService.getAllEvents()).thenReturn(List.of(sampleEvent));

        mockMvc.perform(get("/tracking/events")
                        .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].msisdn").value("771234567"));

        verify(trackingService).getAllEvents();
    }

    @Test
    @DisplayName("Devrait retourner 401 Unauthorized pour getEventsByMsisdn sans rôle")
    void shouldRejectGetEventsByMsisdnWhenXUserRoleIsNull() throws Exception {
        mockMvc.perform(get("/tracking/events/771234567"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devrait retourner 403 Forbidden si un CLIENT demande les événements d'un autre numéro")
    void shouldRejectGetEventsByMsisdnWhenClientRequestsAnotherUserPhone() throws Exception {
        mockMvc.perform(get("/tracking/events/779999999")
                        .header("X-User-Phone", "771234567")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Devrait retourner 200 OK si un CLIENT demande ses propres événements")
    void shouldReturnEventsWhenClientRequestsOwnPhone() throws Exception {
        when(trackingService.getEventsByMsisdn("771234567")).thenReturn(List.of(sampleEvent));

        mockMvc.perform(get("/tracking/events/771234567")
                        .header("X-User-Phone", "771234567")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("PURCHASE_PASS"));

        verify(trackingService).getEventsByMsisdn("771234567");
    }

    @Test
    @DisplayName("Devrait retourner 200 OK si un ADMINISTRATOR demande les événements de n'importe quel numéro")
    void shouldReturnEventsWhenAdministratorRequestsAnyPhone() throws Exception {
        when(trackingService.getEventsByMsisdn("779999999")).thenReturn(List.of(sampleEvent));

        mockMvc.perform(get("/tracking/events/779999999")
                        .header("X-User-Phone", "770000000")
                        .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk());

        verify(trackingService).getEventsByMsisdn("779999999");
    }
}
