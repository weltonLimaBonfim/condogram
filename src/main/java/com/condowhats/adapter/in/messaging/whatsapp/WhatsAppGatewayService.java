package com.condowhats.adapter.in.messaging.whatsapp;

import com.condowhats.adapter.in.messaging.whatsapp.WhatsAppUpdateParser.ParsedUpdate;
import com.condowhats.domain.event.BotMessageReceived;
import com.condowhats.domain.model.*;
import com.condowhats.domain.port.Channel;
import com.condowhats.domain.repository.BotSessionRepository;
import com.condowhats.domain.repository.ChannelConfigRepository;
import com.condowhats.domain.repository.ResidentChannelRepository;
import com.condowhats.domain.repository.ResidentRepository;
import com.condowhats.service.EventStoreService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppGatewayService {

    private final ApplicationEventPublisher eventBus;
    private final EventStoreService eventStore;
    private final WhatsAppUpdateParser parser;
    private final ChannelConfigRepository channelConfigRepo;
    private final ResidentRepository residentRepo;
    private final ResidentChannelRepository residentChannelRepo;
    private final BotSessionRepository sessionRepo;

    @Transactional
    public void processWebhook(JsonNode payload, String phoneNumber) {
        ChannelConfig config = channelConfigRepo
                .findByPublicIdentifierAndChannel(phoneNumber, Channel.WHATSAPP)
                .orElseThrow(() -> new IllegalStateException("Número não encontrado: " + phoneNumber));

        Condominium condo = config.getCondominium();
        if (condo != null && condo.getStatus() != Condominium.CondoStatus.ACTIVE) {
            log.warn("Webhook ignorado — condomínio inativo: {}", condo.getId());
            return;
        }

        ParsedUpdate parsed = parser.parse(payload).orElse(null);
        if (parsed == null) return;

        // Busca sessão ativa pelo número de telefone
        BotSession session = sessionRepo
                .findActiveByExternalIdAndChannel(parsed.fromPhone(), Channel.WHATSAPP, Instant.now())
                .orElseGet(() -> createNewSession(config, parsed));

        if (session.isIdentified()) {
            Resident resident = session.getResident();
            if (resident.getStatus() == Resident.ResidentStatus.BLOCKED) {
                log.info("Mensagem de residente bloqueado ignorada: phone={}", parsed.fromPhone());
                return;
            }
        }

        eventStore.saveInbound(
                session.getCondominium(), session.getResident(), session,
                parsed.messageId(), parsed.messageType(),
                Map.of("content", parsed.content(), "from", parsed.fromPhone(), "channel", "WHATSAPP")
        );

        eventBus.publishEvent(new BotMessageReceived(
                session.getCondominium() != null ? session.getCondominium().getId() : null,
                session.getResident().getId(),
                session.getId(),
                Channel.WHATSAPP,
                parsed.messageId(),
                parsed.messageType(),
                parsed.content(),
                payload.toString(),
                Instant.now()
        ));

        log.debug("WhatsApp update processado | phone={} state={}", parsed.fromPhone(), session.getState());
    }

    private BotSession createNewSession(ChannelConfig config, ParsedUpdate parsed) {
        return residentChannelRepo
                .findByExternalIdAndChannel(parsed.fromPhone(), Channel.WHATSAPP)
                .map(rc -> sessionRepo.save(BotSession.builder()
                        .resident(rc.getResident())
                        .condominium(rc.getResident().getCondominium())
                        .channel(Channel.WHATSAPP)
                        .state(BotState.IDENTIFYING.name())
                        .build()))
                .orElseGet(() -> {
                    Resident placeholder = residentRepo.save(Resident.builder()
                            .condominium(config.getSharedBot() ? null : config.getCondominium())
                            .name("Morador " + parsed.fromPhone().substring(parsed.fromPhone().length() - 4))
                            .cpf("00000000000")
                            .unitNumber("?")
                            .role(Resident.ResidentRole.RESIDENT)
                            .status(Resident.ResidentStatus.ACTIVE)
                            .build());
                    residentChannelRepo.save(ResidentChannel.builder()
                            .resident(placeholder)
                            .channel(Channel.WHATSAPP)
                            .externalId(parsed.fromPhone())
                            .optedIn(false)
                            .build());
                    return sessionRepo.save(BotSession.builder()
                            .resident(placeholder)
                            .condominium(config.getSharedBot() ? null : config.getCondominium())
                            .channel(Channel.WHATSAPP)
                            .state(BotState.IDENTIFYING.name())
                            .build());
                });
    }
}
