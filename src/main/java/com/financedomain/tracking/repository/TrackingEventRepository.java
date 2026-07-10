package com.financedomain.tracking.repository;

import com.financedomain.tracking.dto.TrackingEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingEventRepository extends MongoRepository<TrackingEvent, String> {
    List<TrackingEvent> findByMsisdn(String msisdn);
    List<TrackingEvent> findByEventType(String eventType);
}
