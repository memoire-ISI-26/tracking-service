package com.financedomain.tracking.service;

import com.financedomain.tracking.dto.TrackingEvent;
import com.financedomain.tracking.repository.TrackingEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class TrackingService {

    @Autowired
    private TrackingEventRepository trackingEventRepository;

    /**
     * Reçoit un événement, lui affecte un timestamp s'il est absent,
     * le logue et le stocke en base de données NoSQL.
     */
    public TrackingEvent collect(TrackingEvent event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        log.info("[TRACKING] type={} | msisdn={} | service={} | ts={}",
                event.getEventType(),
                event.getMsisdn(),
                event.getSourceService(),
                event.getTimestamp());

        return trackingEventRepository.save(event);
    }

    /** Retourne tous les événements collectés (utile pour les tests / debug). */
    public List<TrackingEvent> getAllEvents() {
        return trackingEventRepository.findAll();
    }

    /** Retourne les événements filtrés par msisdn. */
    public List<TrackingEvent> getEventsByMsisdn(String msisdn) {
        return trackingEventRepository.findByMsisdn(msisdn);
    }
}
