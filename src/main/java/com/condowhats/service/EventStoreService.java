package com.condowhats.service;

import com.condowhats.domain.model.BotSession;
import com.condowhats.domain.model.Condominium;
import com.condowhats.domain.model.InteractionEvent;
import com.condowhats.domain.model.Resident;
import com.condowhats.domain.repository.InteractionEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventStoreService {

    private final InteractionEventRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InteractionEvent saveInbound(
            Condominium condo, Resident resident, BotSession session,
            String externalMessageId, String eventType, Map<String, Object> payload) {
        return repo.save(InteractionEvent.builder()
                .condominium(condo)
                .resident(resident)
                .session(session)
                .eventType("MSG_INBOUND")
                .direction(InteractionEvent.Direction.INBOUND)
                .payloadJson(payload)
                .externalMessageId(externalMessageId)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InteractionEvent saveStateChange(
            Condominium condo, Resident resident, BotSession session,
            String from, String to, Map<String, Object> payload) {
        return repo.save(InteractionEvent.builder()
                .condominium(condo)
                .resident(resident)
                .session(session)
                .eventType("SESSION_STATE_CHANGED")
                .direction(InteractionEvent.Direction.INTERNAL)
                .payloadJson(payload)
                .previousState(from)
                .nextState(to)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InteractionEvent saveOutbound(
            Condominium condo, Resident resident,
            String externalMessageId, String eventType, Map<String, Object> payload) {
        return repo.save(InteractionEvent.builder()
                .condominium(condo)
                .resident(resident)
                .eventType(eventType)
                .direction(InteractionEvent.Direction.OUTBOUND)
                .payloadJson(payload)
                .externalMessageId(externalMessageId)
                .build());
    }
}
