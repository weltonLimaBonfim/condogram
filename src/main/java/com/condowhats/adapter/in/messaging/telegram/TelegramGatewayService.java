package com.condowhats.adapter.in.messaging.telegram;

import com.condowhats.adapter.in.messaging.telegram.TelegramUpdateParser.ParsedUpdate;
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
public class TelegramGatewayService {

    private final ApplicationEventPublisher eventBus;
    private final EventStoreService eventStore;
    private final TelegramUpdateParser parser;
    private final ChannelConfigRepository channelConfigRepo;
    private final ResidentRepository residentRepo;
    private final ResidentChannelRepository residentChannelRepo;
    private final BotSessionRepository sessionRepo;

    @Transactional
    public void processUpdate(JsonNode update, String botUsername) {

        // 1. Localiza config do bot pelo publicIdentifier
        ChannelConfig config = channelConfigRepo
                .findByPublicIdentifierAndChannel(botUsername, Channel.TELEGRAM)
                .orElseThrow(() -> new IllegalStateException("Bot não encontrado: @" + botUsername));

        // 2. Parseia o update
        ParsedUpdate parsed = parser.parse(update).orElse(null);
        if (parsed == null) return;

        // 3. Tenta encontrar sessão ativa pelo externalId
        BotSession session = sessionRepo
                .findActiveByExternalIdAndChannel(parsed.chatId(), Channel.TELEGRAM, Instant.now())
                .orElseGet(() -> createNewSession(config, parsed));

        // 4. Valida status do morador se já identificado
        if (session.isIdentified()) {
            Resident resident = session.getResident();
            if (resident.getStatus() == Resident.ResidentStatus.BLOCKED) {
                log.info("Mensagem de residente bloqueado ignorada: chatId={}", parsed.chatId());
                return;
            }
            if (session.getCondominium().getStatus() != Condominium.CondoStatus.ACTIVE) {
                log.warn("Condomínio inativo: {}", session.getCondominium().getId());
                return;
            }
        }

        // 5. Persiste o evento de interação
        eventStore.saveInbound(
                session.getCondominium(),
                session.getResident(),
                session,
                update.path("update_id").asText(),
                parsed.messageType(),
                Map.of("content", parsed.content(), "chatId", parsed.chatId(), "channel", "TELEGRAM")
        );

        // 6. Publica evento de domínio
        eventBus.publishEvent(new BotMessageReceived(
                session.getCondominium() != null ? session.getCondominium().getId() : null,
                session.getResident().getId(),
                session.getId(),
                Channel.TELEGRAM,
                update.path("update_id").asText(),
                parsed.messageType(),
                parsed.content(),
                update.toString(),
                Instant.now()
        ));

        log.debug("Update processado | chatId={} state={} identified={}",
                parsed.chatId(), session.getState(), session.isIdentified());
    }

    private BotSession createNewSession(ChannelConfig config, ParsedUpdate parsed) {
        log.info("Nova sessão | chatId={} sharedBot={}", parsed.chatId(), config.getSharedBot());

        // Se o chatId já tem ResidentChannel (sessão expirada), restaura vínculo
        return residentChannelRepo
                .findByExternalIdAndChannel(parsed.chatId(), Channel.TELEGRAM)
                .map(rc -> sessionRepo.save(BotSession.builder()
                        .resident(rc.getResident())
                        .condominium(rc.getResident().getCondominium())
                        .channel(Channel.TELEGRAM)
                        .state(BotState.IDENTIFYING.name())
                        .build()))
                .orElseGet(() -> {
                    // Visitante novo — cria placeholder
                    Resident placeholder = createPlaceholder(config, parsed);
                    return sessionRepo.save(BotSession.builder()
                            .resident(placeholder)
                            .condominium(config.getSharedBot() ? null : config.getCondominium())
                            .channel(Channel.TELEGRAM)
                            .state(BotState.IDENTIFYING.name())
                            .build());
                });
    }

    private Resident createPlaceholder(ChannelConfig config, ParsedUpdate parsed) {
        Resident placeholder = residentRepo.save(Resident.builder()
                .condominium(config.getSharedBot() ? null : config.getCondominium())
                .name(parsed.firstName() != null ? parsed.firstName() : "Visitante")
                .cpf("00000000000")
                .unitNumber("?")
                .role(Resident.ResidentRole.RESIDENT)
                .status(Resident.ResidentStatus.ACTIVE)
                .build());

        residentChannelRepo.save(ResidentChannel.builder()
                .resident(placeholder)
                .channel(Channel.TELEGRAM)
                .externalId(parsed.chatId())
                .displayHandle(parsed.username())
                .optedIn(false)
                .build());

        return placeholder;
    }
}
