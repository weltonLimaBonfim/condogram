package com.condowhats.infrastructure.scheduler;

import com.condowhats.domain.model.BotState;
import com.condowhats.domain.repository.BotSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionExpiryScheduler {

    private final BotSessionRepository sessionRepo;

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireSessions() {
        var expired = sessionRepo.findByExpiresAtBefore(Instant.now());
        if (expired.isEmpty()) return;

        expired.forEach(session -> {
            session.setBotState(BotState.IDENTIFYING);
            session.getContextData().clear();
            session.setCondominium(null);
        });
        sessionRepo.saveAll(expired);
        log.info("Sessões expiradas: {} resetadas para IDENTIFYING", expired.size());
    }
}
