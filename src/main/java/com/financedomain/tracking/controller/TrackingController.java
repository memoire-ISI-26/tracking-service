package com.financedomain.tracking.controller;

import com.financedomain.tracking.dto.TrackingEvent;
import com.financedomain.tracking.service.TrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tracking")
public class TrackingController {

    private static final String UNAUTHORIZED = "Unauthorized";
    private static final String ACCESSDENIED = "Access Denied";
    private static final String ADMINISTRATOR = "ADMINISTRATOR";

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Endpoint principal : reçoit un événement depuis n'importe quel microservice.
     * Accessible via appels Feign internes (rôle INTERNAL) ou depuis la gateway.
     */
    @PostMapping("/event")
    public ResponseEntity<?> collectEvent(
            @RequestBody TrackingEvent event,
            @RequestHeader(value = "X-User-Role", required = false) String xUserRole) {
        if (xUserRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(UNAUTHORIZED);
        }
        TrackingEvent saved = trackingService.collect(event);
        try {
            kafkaTemplate.send("tracking-events", saved.getMsisdn(), saved);
        } catch (Exception e) {
            System.err.println("Erreur d'envoi de l'événement vers Kafka : " + e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Consultation de tous les événements — réservé à l'administrateur.
     */
    @GetMapping("/events")
    public ResponseEntity<?> getAllEvents(
            @RequestHeader(value = "X-User-Role", required = false) String xUserRole) {
        if (xUserRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(UNAUTHORIZED);
        }
        if (!ADMINISTRATOR.equals(xUserRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ACCESSDENIED);
        }
        List<TrackingEvent> events = trackingService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Consultation des événements par msisdn.
     * Un CLIENT ne peut voir que ses propres événements.
     */
    @GetMapping("/events/{msisdn}")
    public ResponseEntity<?> getEventsByMsisdn(
            @PathVariable String msisdn,
            @RequestHeader(value = "X-User-Phone", required = false) String xUserPhone,
            @RequestHeader(value = "X-User-Role", required = false) String xUserRole) {
        if (xUserRole == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(UNAUTHORIZED);
        }
        if ("CLIENT".equals(xUserRole) && !msisdn.equals(xUserPhone)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ACCESSDENIED);
        }
        return ResponseEntity.ok(trackingService.getEventsByMsisdn(msisdn));
    }
}
