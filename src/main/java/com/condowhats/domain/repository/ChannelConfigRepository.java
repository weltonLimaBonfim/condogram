package com.condowhats.domain.repository;

import com.condowhats.domain.model.ChannelConfig;
import com.condowhats.domain.port.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelConfigRepository extends JpaRepository<ChannelConfig, Long> {
    Optional<ChannelConfig> findByCondominiumIdAndChannelAndActiveTrue(Long condoId, Channel channel);

    Optional<ChannelConfig> findByPublicIdentifierAndChannel(String publicIdentifier, Channel channel);

    /**
     * Busca bot compartilhado (shared) ativo para um canal
     */
    Optional<ChannelConfig> findByChannelAndSharedBotTrueAndActiveTrue(Channel channel);
}
