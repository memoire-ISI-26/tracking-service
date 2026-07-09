package com.financedomain.tracking.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingEvent {

    /** Type d'événement : LOGIN, PURCHASE_PASS, PURCHASE_CREDIT, VIEW_USAGE, LOGOUT, etc. */
    private String eventType;

    /** Numéro MSISDN de l'utilisateur émetteur de l'événement */
    private String msisdn;

    /** Identifiant interne de l'utilisateur (issu de X-User-Id) */
    private String userId;

    /** Rôle de l'utilisateur (CLIENT, ADMINISTRATOR) */
    private String userRole;

    /** Microservice source de l'événement (ex: pricing-service, user-service) */
    private String sourceService;

    /** Données supplémentaires libres (ex: nom du pass acheté, montant, etc.) */
    private Object payload;

    /** Horodatage de l'événement (rempli automatiquement si absent) */
    private Instant timestamp;
}
