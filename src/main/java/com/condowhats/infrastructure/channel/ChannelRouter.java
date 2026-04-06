package com.condowhats.infrastructure.channel;

import com.condowhats.domain.model.ChannelConfig;
import com.condowhats.domain.model.Condominium;
import com.condowhats.domain.model.Resident;
import com.condowhats.domain.model.ResidentChannel;
import com.condowhats.domain.port.Channel;
import com.condowhats.domain.port.MessagingChannel;
import com.condowhats.domain.port.OutboundMessage;
import com.condowhats.domain.repository.ChannelConfigRepository;
import com.condowhats.domain.repository.ResidentChannelRepository;
import com.condowhats.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChannelRouter {

    private final Map<Channel, MessagingChannel> channelMap;
    private final ChannelConfigRepository configRepo;
    private final ResidentChannelRepository residentChannelRepo;

    /**
     * @Autowired é necessário aqui porque o Spring injeta List<MessagingChannel>
     * (todos os beans que implementam a interface) — sem @Autowired o construtor
     * manual não é reconhecido para injeção de dependência.
     */
    @Autowired
    public ChannelRouter(List<MessagingChannel> channels,
                         ChannelConfigRepository configRepo,
                         ResidentChannelRepository residentChannelRepo) {
        this.channelMap = channels.stream()
                .collect(Collectors.toMap(MessagingChannel::channel, Function.identity()));
        this.configRepo = configRepo;
        this.residentChannelRepo = residentChannelRepo;
        log.info("ChannelRouter iniciado com canais: {}", this.channelMap.keySet());
    }

    public Mono<String> send(Condominium condo, Resident resident, Channel channel, String text) {
        String extId = resolveExternalId(resident, channel);
        return dispatch(resolveConfig(condo, channel), channel, OutboundMessage.text(extId, text));
    }

    public Mono<String> sendWithButtons(Condominium condo, Resident resident, Channel channel,
                                        String text, List<OutboundMessage.ButtonRow> rows) {
        String extId = resolveExternalId(resident, channel);
        return dispatch(resolveConfig(condo, channel), channel, OutboundMessage.withButtons(extId, text, rows));
    }

    public Mono<String> sendNotification(Condominium condo, Resident resident, Channel channel,
                                         OutboundMessage message) {
        ChannelConfig config = resolveConfig(condo, channel);
        return resolveImpl(channel).sendNotification(config, message)
                .doOnError(e -> log.error("Falha na notificação [canal={}]: {}", channel, e.getMessage()))
                .onErrorComplete();
    }

    /**
     * Envia sem condomínio definido — usa o bot shared (pré-identificação)
     */
    public Mono<String> sendRaw(Resident resident, Channel channel, String text) {
        ChannelConfig config = configRepo.findByChannelAndSharedBotTrueAndActiveTrue(channel)
                .orElseThrow(() -> new ResourceNotFoundException("Bot compartilhado", channel.name()));
        String extId = resolveExternalId(resident, channel);
        return resolveImpl(channel).send(config, OutboundMessage.text(extId, text))
                .doOnError(e -> log.error("Falha sendRaw [canal={}]: {}", channel, e.getMessage()))
                .onErrorComplete();
    }

    /**
     * Envia com botões sem condomínio definido — usa o bot shared (pré-identificação)
     */
    public Mono<String> sendRawWithButtons(Resident resident, Channel channel,
                                           String text, List<OutboundMessage.ButtonRow> rows) {
        ChannelConfig config = configRepo.findByChannelAndSharedBotTrueAndActiveTrue(channel)
                .orElseThrow(() -> new ResourceNotFoundException("Bot compartilhado", channel.name()));
        String extId = resolveExternalId(resident, channel);
        return resolveImpl(channel).send(config, OutboundMessage.withButtons(extId, text, rows))
                .doOnError(e -> log.error("Falha sendRawWithButtons [canal={}]: {}", channel, e.getMessage()))
                .onErrorComplete();
    }

    private Mono<String> dispatch(ChannelConfig config, Channel channel, OutboundMessage msg) {
        return resolveImpl(channel).send(config, msg)
                .doOnError(e -> log.error("Falha no envio [canal={}]: {}", channel, e.getMessage()))
                .onErrorComplete();
    }

    private MessagingChannel resolveImpl(Channel channel) {
        MessagingChannel impl = channelMap.get(channel);
        if (impl == null) throw new IllegalStateException("Canal não suportado: " + channel);
        return impl;
    }

    private ChannelConfig resolveConfig(Condominium condo, Channel channel) {
        if (condo == null) {
            return configRepo.findByChannelAndSharedBotTrueAndActiveTrue(channel)
                    .orElseThrow(() -> new ResourceNotFoundException("Config de canal (shared)", channel.name()));
        }
        return configRepo.findByCondominiumIdAndChannelAndActiveTrue(condo.getId(), channel)
                .or(() -> configRepo.findByChannelAndSharedBotTrueAndActiveTrue(channel))
                .orElseThrow(() -> new ResourceNotFoundException("Config de canal",
                        channel + " / condo " + condo.getId()));
    }

    private String resolveExternalId(Resident resident, Channel channel) {
        return residentChannelRepo.findByResidentIdAndChannel(resident.getId(), channel)
                .map(ResidentChannel::getExternalId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentChannel",
                        resident.getId() + "/" + channel));
    }
}
