package com.condowhats.service.notification;

import com.condowhats.domain.event.OccurrenceCreated;
import com.condowhats.domain.event.ReservationCreated;
import com.condowhats.domain.model.Resident;
import com.condowhats.domain.model.ResidentChannel;
import com.condowhats.domain.port.Channel;
import com.condowhats.domain.port.MessageBuilder;
import com.condowhats.domain.repository.CondominiumRepository;
import com.condowhats.domain.repository.ResidentChannelRepository;
import com.condowhats.domain.repository.ResidentRepository;
import com.condowhats.infrastructure.channel.ChannelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationService {

    private final ResidentRepository residentRepo;
    private final CondominiumRepository condoRepo;
    private final ResidentChannelRepository residentChannelRepo;
    private final ChannelRouter router;

    @Async
    @EventListener
    public void onOccurrenceCreated(OccurrenceCreated event) {
        resolve(event.residentId(), event.condominiumId(), (resident, condo) ->
                sendToAllChannels(resident, condo,
                        MessageBuilder.occurrenceOpened("", event.ticketNumber(), event.ticketNumber()))
        );
    }

    @Async
    @EventListener
    public void onReservationCreated(ReservationCreated event) {
        resolve(event.residentId(), event.condominiumId(), (resident, condo) ->
                sendToAllChannels(resident, condo,
                        MessageBuilder.reservationConfirmed("",
                                event.areaName(),
                                event.reservationDate().toString(),
                                event.startTime() + " às " + event.endTime()))
        );
    }

    /**
     * Envia comunicado para todos os moradores com opt-in em todos os canais ativos.
     */
    public void sendAnnouncement(Long condoId, String subject, String body) {
        condoRepo.findById(condoId).ifPresent(condo ->
                residentRepo.findByCondominiumId(condoId,
                                org.springframework.data.domain.Pageable.unpaged()).stream()
                        .filter(r -> r.getStatus() == Resident.ResidentStatus.ACTIVE)
                        .forEach(resident -> sendToAllChannels(resident, condo,
                                MessageBuilder.announcement("", subject, body)))
        );
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    /**
     * Envia para todos os canais ativos do morador.
     * O recipientId no OutboundMessage é preenchido para cada canal.
     */
    private void sendToAllChannels(Resident resident,
                                   com.condowhats.domain.model.Condominium condo,
                                   com.condowhats.domain.port.OutboundMessage template) {
        Arrays.stream(Channel.values()).forEach(channel -> {
            residentChannelRepo.findByResidentIdAndChannel(resident.getId(), channel)
                    .filter(ResidentChannel::getOptedIn)
                    .ifPresent(rc -> {
                        var msg = new com.condowhats.domain.port.OutboundMessage(
                                rc.getExternalId(), template.text(), template.buttons()
                        );
                        router.sendNotification(condo, resident, channel, msg)
                                .subscribe();
                    });
        });
    }

    private void resolve(Long residentId, Long condoId, Action action) {
        residentRepo.findById(residentId).ifPresent(r ->
                condoRepo.findById(condoId).ifPresent(c -> action.run(r, c))
        );
    }

    @FunctionalInterface
    interface Action {
        void run(Resident r, com.condowhats.domain.model.Condominium c);
    }
}
