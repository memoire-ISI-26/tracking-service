package com.financedomain.tracking.service;

import com.financedomain.tracking.dto.TrackingEvent;
import com.financedomain.tracking.repository.TrackingEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @InjectMocks
    private TrackingService trackingService;

    private TrackingEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = TrackingEvent.builder()
                .id("evt123")
                .eventType("PURCHASE_PASS")
                .msisdn("771234567")
                .userId("1")
                .userRole("CLIENT")
                .sourceService("pricing-service")
                .payload(Map.of("passId", 10, "nom", "Pass Internet 1Go"))
                .build();
    }

    @Test
    @DisplayName("Devrait générer un timestamp s'il est absent et sauvegarder l'événement")
    void shouldAssignTimestampIfMissingAndSaveEvent() {
        assertNull(sampleEvent.getTimestamp());

        when(trackingEventRepository.save(any(TrackingEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrackingEvent saved = trackingService.collect(sampleEvent);

        assertNotNull(saved.getTimestamp());
        assertEquals("771234567", saved.getMsisdn());
        verify(trackingEventRepository).save(sampleEvent);
    }

    @Test
    @DisplayName("Devrait conserver le timestamp existant s'il est déjà renseigné")
    void shouldKeepTimestampIfPresentAndSaveEvent() {
        Instant fixedTs = Instant.now().minusSeconds(100);
        sampleEvent.setTimestamp(fixedTs);

        when(trackingEventRepository.save(any(TrackingEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrackingEvent saved = trackingService.collect(sampleEvent);

        assertEquals(fixedTs, saved.getTimestamp());
        verify(trackingEventRepository).save(sampleEvent);
    }

    @Test
    @DisplayName("Devrait récupérer tous les événements de suivi")
    void shouldGetAllEvents() {
        when(trackingEventRepository.findAll()).thenReturn(List.of(sampleEvent));

        List<TrackingEvent> events = trackingService.getAllEvents();

        assertEquals(1, events.size());
        assertEquals("PURCHASE_PASS", events.get(0).getEventType());
        verify(trackingEventRepository).findAll();
    }

    @Test
    @DisplayName("Devrait récupérer les événements filtrés par numéro MSISDN")
    void shouldGetEventsByMsisdn() {
        when(trackingEventRepository.findByMsisdn("771234567")).thenReturn(List.of(sampleEvent));

        List<TrackingEvent> events = trackingService.getEventsByMsisdn("771234567");

        assertEquals(1, events.size());
        assertEquals("771234567", events.get(0).getMsisdn());
        verify(trackingEventRepository).findByMsisdn("771234567");
    }
}
