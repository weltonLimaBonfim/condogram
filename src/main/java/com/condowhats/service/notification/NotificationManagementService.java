package com.condowhats.service.notification;

import com.condowhats.domain.repository.CondominiumRepository;
import com.condowhats.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationManagementService {

    private final NotificationService notificationService;
    private final CondominiumRepository condoRepo;

    public void sendAnnouncement(Long condoId, String subject, String body) {
        if (!condoRepo.existsById(condoId)) {
            throw new ResourceNotFoundException("Condomínio", condoId);
        }
        log.info("Enviando comunicado: condoId={} subject={}", condoId, subject);
        notificationService.sendAnnouncement(condoId, subject, body);
    }
}
