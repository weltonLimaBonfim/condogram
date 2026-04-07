package com.condowhats.service.bot;

import com.condowhats.domain.event.BotMessageReceived;
import com.condowhats.domain.event.OccurrenceCreationRequested;
import com.condowhats.domain.event.ReservationConflicted;
import com.condowhats.domain.event.ReservationRequested;
import com.condowhats.domain.model.BotSession;
import com.condowhats.domain.model.BotState;
import com.condowhats.domain.model.Occurrence.OccurrenceCategory;
import com.condowhats.domain.model.ResidentChannel;
import com.condowhats.domain.port.MessageBuilder;
import com.condowhats.domain.port.OutboundMessage;
import com.condowhats.domain.repository.BotSessionRepository;
import com.condowhats.domain.repository.CommonAreaRepository;
import com.condowhats.domain.repository.ResidentChannelRepository;
import com.condowhats.infrastructure.channel.ChannelRouter;
import com.condowhats.service.EventStoreService;
import com.condowhats.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotOrchestratorService {

    private final ApplicationEventPublisher eventBus;
    private final EventStoreService eventStore;
    private final BotSessionRepository sessionRepo;
    private final CommonAreaRepository areaRepo;
    private final ResidentChannelRepository residentChannelRepo;
    private final ChannelRouter router;
    private final ReservationService reservationService;
    private final IdentificationService identificationService;

    //@Async
    @EventListener
    @Transactional
    public void onMessage(BotMessageReceived event) {
        BotSession session = sessionRepo.findById(event.sessionId()).orElseThrow();

        if (session.isExpired()) {
            session.setBotState(BotState.IDENTIFYING);
            session.getContextData().clear();
            session.setCondominium(null);
            sessionRepo.save(session);
        }

        String prev = session.getState();
        BotState next = transition(session, event);

        session.setBotState(next);
        session.touch();
        sessionRepo.save(session);

        if (session.getCondominium() != null) {
            eventStore.saveStateChange(
                    session.getCondominium(), session.getResident(), session,
                    prev, next.name(),
                    Map.of("input", event.content(), "channel", event.channel().name())
            );
        }
    }

    private BotState transition(BotSession session, BotMessageReceived event) {
        String input = event.content().trim();
        BotState state = session.getBotState();

        // ── Fluxo de identificação por CPF ────────────────────────────────────
        if (state.needsIdentification()) {
            return handleIdentification(session, input);
        }

        // ── Escolha de condomínio (múltiplos) ─────────────────────────────────
        if (input.startsWith("CONDO_")) {
            if (identificationService.handleCondoChoice(session, input)) {
                sendMainMenu(session);
                return BotState.MAIN_MENU;
            }
            return session.getBotState();
        }

        // ── Comandos globais — funcionam em qualquer estado ───────────────────
        if ("/start".equalsIgnoreCase(input) || "/menu".equalsIgnoreCase(input)) {
            sendMainMenu(session);
            return BotState.MAIN_MENU;
        }

        if ("/sair".contains(input) || "/logout".equalsIgnoreCase(input)) {
            session.setCondominium(null);
            session.getContextData().clear();
            sendText(session, "Sessão encerrada. Envie qualquer mensagem para começar novamente.");
            return BotState.IDENTIFYING;
        }

        // ── FSM principal (morador já identificado) ───────────────────────────
        return switch (state) {
            case MAIN_MENU -> handleMainMenu(session, input);
            case OCCURRENCE_TITLE -> handleOccurrenceTitle(session, input);
            case OCCURRENCE_CATEGORY -> handleOccurrenceCategory(session, input);
            case OCCURRENCE_DESC -> handleOccurrenceDesc(session, input);
            case OCCURRENCE_CONFIRM -> handleOccurrenceConfirm(session, input);
            case RESERVATION_AREA -> handleReservationArea(session, input);
            case RESERVATION_DATE -> handleReservationDate(session, input);
            case RESERVATION_TIME -> handleReservationTime(session, input);
            case RESERVATION_CONFIRM -> handleReservationConfirm(session, input);
            case AWAITING_HUMAN -> BotState.AWAITING_HUMAN;
            default -> fallback(session);
        };
    }

    // ── Identificação ─────────────────────────────────────────────────────────

    private BotState handleIdentification(BotSession session, String input) {
        // Primeira mensagem (IDLE/IDENTIFYING sem tentativas) → pede CPF
        if (!session.getContextData().containsKey("cpfAttempts") &&
                session.getBotState() != BotState.CPF_NOT_FOUND) {
            identificationService.requestCpf(session);
            return BotState.IDENTIFYING;
        }

        // Tentativa de CPF
        boolean identified = identificationService.handleCpfInput(session, input);
        if (identified) {
            sendMainMenu(session);
            return BotState.MAIN_MENU;
        }
        return session.getBotState(); // IDENTIFYING ou CPF_NOT_FOUND
    }

    // ── Menu e navegação ──────────────────────────────────────────────────────

    private BotState handleMainMenu(BotSession session, String input) {
        return switch (input.toUpperCase()) {
            case "MENU_OCCURRENCE", "/OCCURRENCE" -> {
                sendText(session, "Qual é o *título* da ocorrência?\n_(Ex: Barulho excessivo no 3° andar)_");
                yield BotState.OCCURRENCE_TITLE;
            }
            case "MENU_RESERVATION", "/RESERVATION" -> {
                sendAreaList(session);
                yield BotState.RESERVATION_AREA;
            }
            case "MENU_MY_OCCURRENCES" -> {
                sendText(session, "Em breve: consulta de ocorrências. Entre em contato com a administração.");
                sendMainMenu(session);
                yield BotState.MAIN_MENU;
            }
            default -> {
                sendMainMenu(session);
                yield BotState.MAIN_MENU;
            }
        };
    }

    // ── Ocorrências ───────────────────────────────────────────────────────────

    private BotState handleOccurrenceTitle(BotSession session, String input) {
        if (input.length() < 5) {
            sendText(session, "⚠️ Mínimo 5 caracteres.");
            return BotState.OCCURRENCE_TITLE;
        }
        session.getContextData().put("occTitle", input);
        send(session, MessageBuilder.categoryPicker(extId(session)));
        return BotState.OCCURRENCE_CATEGORY;
    }

    private BotState handleOccurrenceCategory(BotSession session, String input) {
        try {
            OccurrenceCategory cat = OccurrenceCategory.valueOf(input.toUpperCase());
            session.getContextData().put("occCategory", cat.name());
            sendText(session, "Descreva a ocorrência com mais detalhes:");
            return BotState.OCCURRENCE_DESC;
        } catch (IllegalArgumentException e) {
            sendText(session, "⚠️ Opção inválida. Use os botões.");
            send(session, MessageBuilder.categoryPicker(extId(session)));
            return BotState.OCCURRENCE_CATEGORY;
        }
    }

    private BotState handleOccurrenceDesc(BotSession session, String input) {
        session.getContextData().put("occDesc", input);
        String summary = "*Confirmar registro?*\n\n📋 %s\n🏷 %s\n📝 %s".formatted(
                session.getContextData().get("occTitle"),
                session.getContextData().get("occCategory"),
                input
        );
        send(session, MessageBuilder.confirmationButtons(extId(session), summary));
        return BotState.OCCURRENCE_CONFIRM;
    }

    private BotState handleOccurrenceConfirm(BotSession session, String input) {
        if ("CONFIRM_YES".equals(input)) {
            eventBus.publishEvent(new OccurrenceCreationRequested(
                    session.getCondominium().getId(), session.getResident().getId(),
                    session.getId(), null,
                    (String) session.getContextData().get("occTitle"),
                    (String) session.getContextData().get("occDesc"),
                    OccurrenceCategory.valueOf(
                            (String) session.getContextData().getOrDefault("occCategory", "OTHER"))
            ));
            session.getContextData().clear();
            sendText(session, "⏳ Registrando... protocolo chegará em instantes.");
        } else {
            sendText(session, "❌ Ocorrência cancelada.");
            sendMainMenu(session);
            session.getContextData().clear();
        }
        return BotState.MAIN_MENU;
    }

    // ── Reservas ──────────────────────────────────────────────────────────────

    private BotState handleReservationArea(BotSession session, String input) {
        if (!input.startsWith("AREA_")) {
            sendAreaList(session);
            return BotState.RESERVATION_AREA;
        }
        try {
            Long areaId = Long.parseLong(input.substring(5));
            areaRepo.findById(areaId).ifPresentOrElse(
                    area -> {
                        session.getContextData().put("areaId", areaId);
                        session.getContextData().put("areaName", area.getName());
                        sendText(session, "✅ Área: *" + area.getName() + "*\n\nQual a *data*?\n_(DD/MM/AAAA)_");
                    },
                    () -> sendText(session, "Área não encontrada.")
            );
            return BotState.RESERVATION_DATE;
        } catch (NumberFormatException e) {
            sendAreaList(session);
            return BotState.RESERVATION_AREA;
        }
    }

    private BotState handleReservationDate(BotSession session, String input) {
        Optional<LocalDate> date = reservationService.parseDate(input);
        if (date.isEmpty()) {
            sendText(session, "⚠️ Data inválida. Use DD/MM/AAAA.");
            return BotState.RESERVATION_DATE;
        }
        if (date.get().isBefore(LocalDate.now())) {
            sendText(session, "⚠️ Data no passado.");
            return BotState.RESERVATION_DATE;
        }
        session.getContextData().put("resDate", input);
        sendText(session, "Qual o *horário*?\n_(Ex: 14:00 às 18:00)_");
        return BotState.RESERVATION_TIME;
    }

    private BotState handleReservationTime(BotSession session, String input) {
        Optional<LocalTime[]> times = reservationService.parseTimeRange(input);
        if (times.isEmpty()) {
            sendText(session, "⚠️ Use HH:mm às HH:mm.");
            return BotState.RESERVATION_TIME;
        }
        session.getContextData().put("resStart", times.get()[0].toString());
        session.getContextData().put("resEnd", times.get()[1].toString());
        String summary = "*Confirmar reserva?*\n\n📍 %s\n📅 %s\n🕐 %s às %s".formatted(
                session.getContextData().get("areaName"), session.getContextData().get("resDate"),
                times.get()[0], times.get()[1]
        );
        send(session, MessageBuilder.confirmationButtons(extId(session), summary));
        return BotState.RESERVATION_CONFIRM;
    }

    private BotState handleReservationConfirm(BotSession session, String input) {
        if ("CONFIRM_YES".equals(input)) {
            eventBus.publishEvent(new ReservationRequested(
                    session.getCondominium().getId(), session.getResident().getId(),
                    session.getId(), null,
                    (Long) session.getContextData().get("areaId"),
                    LocalDate.parse((String) session.getContextData().get("resDate"),
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    LocalTime.parse((String) session.getContextData().get("resStart")),
                    LocalTime.parse((String) session.getContextData().get("resEnd"))
            ));
            sendText(session, "⏳ Solicitando reserva...");
        } else {
            sendText(session, "❌ Reserva cancelada.");
        }
        session.getContextData().clear();
        sendMainMenu(session);
        return BotState.MAIN_MENU;
    }

    private BotState fallback(BotSession session) {
        sendMainMenu(session);
        return BotState.MAIN_MENU;
    }

    // ── Helpers de envio ──────────────────────────────────────────────────────

    private void sendMainMenu(BotSession session) {
        String greeting = Optional.ofNullable(
                        session.getCondominium() != null ? session.getCondominium().getBotGreeting() : null)
                .orElse("Como posso ajudar?");
        send(session, MessageBuilder.mainMenu(extId(session), greeting));
    }

    private void sendAreaList(BotSession session) {
        List<OutboundMessage.Button> btns = areaRepo
                .findByCondominiumIdAndActiveTrue(session.getCondominium().getId()).stream()
                .map(a -> new OutboundMessage.Button("📍 " + a.getName(), "AREA_" + a.getId()))
                .toList();
        if (btns.isEmpty()) {
            sendText(session, "Sem áreas disponíveis.");
        } else send(session, MessageBuilder.areaList(extId(session), btns));
    }

    private void sendText(BotSession session, String text) {
        try {
            if (session.getCondominium() == null) {
                router.sendRaw(session.getResident(), session.getChannel(), text).block();
            } else {
                router.send(session.getCondominium(), session.getResident(), session.getChannel(), text).block();
            }
        } catch (Exception e) {
            log.error("Falha ao enviar texto | residentId={} canal={}: {}",
                    session.getResident().getId(), session.getChannel(), e.getMessage(), e);
        }
    }

    private void send(BotSession session, OutboundMessage msg) {
        try {
            if (msg.buttons() != null) {
                if (session.getCondominium() == null) {
                    router.sendRawWithButtons(session.getResident(),
                            session.getChannel(), msg.text(), msg.buttons()).block();
                } else {
                    router.sendWithButtons(session.getCondominium(), session.getResident(),
                            session.getChannel(), msg.text(), msg.buttons()).block();
                }
            } else {
                sendText(session, msg.text());
            }
        } catch (Exception e) {
            log.error("Falha ao enviar mensagem com botões | residentId={} canal={}: {}",
                    session.getResident().getId(), session.getChannel(), e.getMessage(), e);
        }
    }

    private String extId(BotSession session) {
        return residentChannelRepo.findByResidentIdAndChannel(
                        session.getResident().getId(), session.getChannel())
                .map(ResidentChannel::getExternalId)
                .orElseThrow();
    }

    @Async
    @EventListener
    @Transactional
    public void onReservationConflicted(ReservationConflicted event) {
        sessionRepo.findActiveByResidentIdAndChannel(
                        event.residentId(), com.condowhats.domain.port.Channel.TELEGRAM, java.time.Instant.now())
                .ifPresent(s -> sendText(s,
                        "⚠️ A área *" + event.areaName() + "* já está ocupada neste horário."));
    }
}
