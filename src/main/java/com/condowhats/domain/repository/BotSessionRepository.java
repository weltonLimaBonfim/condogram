package com.condowhats.domain.repository;

import com.condowhats.domain.model.BotSession;
import com.condowhats.domain.port.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BotSessionRepository extends JpaRepository<BotSession, Long> {

    /**
     * Busca sessão ativa por externalId de canal — usada pelo gateway
     */
    @Query("""
                SELECT s FROM BotSession s
                JOIN ResidentChannel rc ON rc.resident.id = s.resident.id
                WHERE rc.externalId = :externalId
                  AND rc.channel = :channel
                  AND s.channel = :channel
                  AND s.expiresAt > :now
                ORDER BY s.lastActivityAt DESC
            """)
    Optional<BotSession> findActiveByExternalIdAndChannel(String externalId, Channel channel, Instant now);

    /**
     * Para sessões não identificadas (antes do CPF)
     */
    @Query("""
                SELECT s FROM BotSession s
                WHERE s.resident.id = :residentId
                  AND s.channel = :channel
                  AND s.expiresAt > :now
                ORDER BY s.lastActivityAt DESC
            """)
    Optional<BotSession> findActiveByResidentIdAndChannel(Long residentId, Channel channel, Instant now);

    List<BotSession> findByExpiresAtBefore(Instant now);
}
