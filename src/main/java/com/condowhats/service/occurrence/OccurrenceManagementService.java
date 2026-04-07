package com.condowhats.service.occurrence;

import com.condowhats.adapter.in.web.dto.request.UpdateOccurrenceRequest;
import com.condowhats.adapter.in.web.dto.response.OccurrenceResponse;
import com.condowhats.adapter.in.web.dto.response.PageResponse;
import com.condowhats.domain.model.Occurrence.OccurrenceStatus;
import com.condowhats.domain.port.Channel;
import com.condowhats.domain.port.MessageBuilder;
import com.condowhats.domain.repository.OccurrenceRepository;
import com.condowhats.domain.repository.ResidentChannelRepository;
import com.condowhats.exception.BusinessRuleException;
import com.condowhats.exception.ResourceNotFoundException;
import com.condowhats.infrastructure.channel.ChannelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OccurrenceManagementService {

    private static final Set<OccurrenceStatus> TERMINAL =
            Set.of(OccurrenceStatus.CLOSED, OccurrenceStatus.CANCELLED);

    private final OccurrenceRepository occurrenceRepo;
    private final ResidentChannelRepository residentChannelRepo;
    private final ChannelRouter router;

    @Transactional(readOnly = true)
    public PageResponse<OccurrenceResponse> list(Long condoId, int page, int size) {
        return PageResponse.from(
                occurrenceRepo.findByCondominiumIdOrderByCreatedAtDesc(condoId,
                                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                        .map(OccurrenceResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public OccurrenceResponse findById(Long condoId, Long id) {
        return occurrenceRepo.findById(id)
                .filter(o -> o.getCondominium().getId().equals(condoId))
                .map(OccurrenceResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Ocorrência", id));
    }

    @Transactional(readOnly = true)
    public OccurrenceResponse findByTicket(Long condoId, String ticket) {
        return occurrenceRepo.findByCondominiumIdAndTicketNumber(condoId, ticket)
                .map(OccurrenceResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Ocorrência", ticket));
    }

    @Transactional
    public OccurrenceResponse update(Long condoId, Long id, UpdateOccurrenceRequest req) {
        var occurrence = occurrenceRepo.findById(id)
                .filter(o -> o.getCondominium().getId().equals(condoId))
                .orElseThrow(() -> new ResourceNotFoundException("Ocorrência", id));

        OccurrenceStatus newStatus = parseStatus(req.status());
        if (TERMINAL.contains(occurrence.getStatus()))
            throw new BusinessRuleException("Não é possível alterar ocorrência com status " + occurrence.getStatus());

        occurrence.setStatus(newStatus);
        if (newStatus == OccurrenceStatus.RESOLVED) occurrence.setResolvedAt(Instant.now());

        var saved = occurrenceRepo.save(occurrence);
        log.info("Ocorrência atualizada: ticket={} status={}", saved.getTicketNumber(), newStatus);

        if (Boolean.TRUE.equals(req.notifyResident())) {
            var msg = MessageBuilder.occurrenceUpdated("", saved.getTicketNumber(), newStatus.name(), req.note());
            // Notifica em todos os canais ativos do morador
            Arrays.stream(Channel.values()).forEach(ch ->
                    residentChannelRepo.findByResidentIdAndChannel(saved.getResident().getId(), ch)
                            .filter(rc -> rc.getOptedIn())
                            .ifPresent(rc -> {
                                var m = new com.condowhats.domain.port.OutboundMessage(rc.getExternalId(), msg.text(), msg.buttons());
                                router.sendNotification(saved.getCondominium(), saved.getResident(), ch, m).block();
                            })
            );
        }
        return OccurrenceResponse.from(saved);
    }

    private OccurrenceStatus parseStatus(String raw) {
        try {
            return OccurrenceStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Status inválido: " + raw);
        }
    }
}
