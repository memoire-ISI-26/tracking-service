package com.financedomain.tracking.service;

import com.financedomain.tracking.dto.TrackingEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TrackingService {

    /**
     * Stockage en mémoire des événements (en attendant Kafka).
     * Remplacé dans la prochaine étape par un producer Kafka.
     */
    private final List<TrackingEvent> eventStore = Collections.synchronizedList(new ArrayList<>());

    /**
     * Reçoit un événement, lui affecte un timestamp s'il est absent,
     * le logue et le stocke localement.
     * TODO (Kafka) : remplacer le stockage local par kafkaTemplate.send(topic, event)
     */
    public TrackingEvent collect(TrackingEvent event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        System.out.printf("[TRACKING] type=%s | msisdn=%s | service=%s | ts=%s%n",
                event.getEventType(),
                event.getMsisdn(),
                event.getSourceService(),
                event.getTimestamp());

        eventStore.add(event);
        return event;
    }

    /** Retourne tous les événements collectés (utile pour les tests / debug). */
    public List<TrackingEvent> getAllEvents() {
        return Collections.unmodifiableList(eventStore);
    }

    /** Retourne les événements filtrés par msisdn. */
    public List<TrackingEvent> getEventsByMsisdn(String msisdn) {
        return eventStore.stream()
                .filter(e -> msisdn.equals(e.getMsisdn()))
                .toList();
    }
}
