package com.condowhats.service.bot;

import com.condowhats.domain.model.BotSession;
import com.condowhats.domain.model.BotState;
import com.condowhats.domain.model.Resident;
import com.condowhats.domain.port.OutboundMessage;
import com.condowhats.domain.repository.BotSessionRepository;
import com.condowhats.domain.repository.ResidentChannelRepository;
import com.condowhats.domain.repository.ResidentRepository;
import com.condowhats.infrastructure.channel.ChannelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentificationService {

    private static final int MAX_ATTEMPTS = 3;

    private final ResidentRepository residentRepo;
    private final ResidentChannelRepository residentChannelRepo;
    private final BotSessionRepository sessionRepo;
    private final ChannelRouter router;

    /**
     * Pede o CPF — chamado na primeira mensagem de uma sessão não identificada.
     */
    public void requestCpf(BotSession session) {
        sendRaw(session, "Olá! Para continuar, informe seu *CPF* (somente números):");
        session.getContextData().put("cpfAttempts", 0);
    }

    @Transactional
    public boolean handleCpfInput(BotSession session, String input) {
        String cpf = Resident.normalizeCpf(input);

        if (cpf == null || cpf.length() != 11 || !cpf.matches("\\d{11}")) {
            sendRaw(session, "CPF inválido. Digite apenas os 11 números.\nExemplo: 12345678901");
            return false;
        }

        List<Resident> found = residentRepo.findAllByCpfAndActive(cpf);

        if (found.isEmpty()) return handleNotFound(session, cpf);
        if (found.size() == 1) return identifyAs(session, found.get(0));
        return handleMultipleCondos(session, found);
    }

    @Transactional
    public boolean handleCondoChoice(BotSession session, String callbackData) {
        if (!callbackData.startsWith("CONDO_")) return false;
        try {
            Long condoId = Long.parseLong(callbackData.substring(6));
            String cpf = (String) session.getContextData().get("pendingCpf");
            if (cpf == null) return false;
            return residentRepo.findByCondominiumIdAndCpf(condoId, cpf)
                    .map(r -> identifyAs(session, r))
                    .orElse(false);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private boolean handleNotFound(BotSession session, String cpf) {
        int attempts = (int) session.getContextData().getOrDefault("cpfAttempts", 0) + 1;
        session.getContextData().put("cpfAttempts", attempts);

        if (attempts >= MAX_ATTEMPTS) {
            sendRaw(session,
                    "CPF não encontrado após " + MAX_ATTEMPTS + " tentativas.\n\n" +
                            "Entre em contato com a administração do seu condomínio para ser cadastrado.\n\n" +
                            "Digite /start para tentar novamente."
            );
            session.setBotState(BotState.CPF_NOT_FOUND);
            log.warn("CPF não encontrado após {} tentativas | residentId={}",
                    MAX_ATTEMPTS, session.getResident().getId());
            return false;
        }

        sendRaw(session, String.format(
                "CPF não encontrado. Tente novamente (%d/%d):\n_(somente os 11 dígitos)_",
                attempts, MAX_ATTEMPTS
        ));
        return false;
    }

    private boolean handleMultipleCondos(BotSession session, List<Resident> residents) {
        session.getContextData().put("pendingCpf", residents.get(0).getCpf());

        List<OutboundMessage.ButtonRow> rows = residents.stream()
                .map(r -> new OutboundMessage.ButtonRow(List.of(
                        new OutboundMessage.Button(
                                r.getCondominium().getName() + " — Apto " + r.getUnitNumber(),
                                "CONDO_" + r.getCondominium().getId()
                        )
                )))
                .toList();

        try {
            router.sendRawWithButtons(session.getResident(), session.getChannel(),
                    "Seu CPF está cadastrado em mais de um condomínio. Selecione:", rows
            ).block();
        } catch (Exception e) {
            log.error("Falha ao enviar lista de condomínios para residentId={}: {}",
                    session.getResident().getId(), e.getMessage(), e);
        }
        return false;
    }

    @Transactional
    boolean identifyAs(BotSession session, Resident identified) {
        Resident placeholder = session.getResident();

        if (!placeholder.getId().equals(identified.getId())) {
            residentChannelRepo
                    .findByResidentIdAndChannel(placeholder.getId(), session.getChannel())
                    .ifPresent(rc -> {
                        rc.setResident(identified);
                        rc.setOptedIn(Boolean.TRUE);
                        rc.setOptedInAt(Instant.now());
                        residentChannelRepo.save(rc);
                        residentChannelRepo.flush();
                    });

            session.setResident(identified);
            sessionRepo.save(session);

            residentRepo.delete(placeholder);
            residentRepo.flush();
        } else {
            residentChannelRepo
                    .findByResidentIdAndChannel(identified.getId(), session.getChannel())
                    .ifPresent(rc -> {
                        if (!Boolean.TRUE.equals(rc.getOptedIn())) {
                            rc.setOptedIn(Boolean.TRUE);
                            rc.setOptedInAt(Instant.now());
                            residentChannelRepo.save(rc);
                        }
                    });
        }

        session.setCondominium(identified.getCondominium());
        session.setBotState(BotState.MAIN_MENU);
        session.getContextData().clear();
        sessionRepo.save(session);

        // Mensagem de boas-vindas — .block() garante envio antes da transação fechar
        String greeting = String.format(
                "Olá, *%s*! Bem-vindo ao *%s* \uD83C\uDFE2\n\nApto: %s%s",
                identified.getName(),
                identified.getCondominium().getName(),
                identified.getUnitNumber(),
                identified.getBlock() != null ? " — Bloco " + identified.getBlock() : ""
        );
        try {
            router.send(
                    identified.getCondominium(), identified, session.getChannel(), greeting
            ).block();
        } catch (Exception e) {
            log.error("Falha ao enviar boas-vindas para residentId={}: {}",
                    identified.getId(), e.getMessage(), e);
        }

        log.info("Morador identificado: residentId={} condo={}",
                identified.getId(), identified.getCondominium().getId());
        return true;
    }

    /**
     * Envia texto pré-identificação via bot shared.
     * Usa .block() — necessário porque estamos numa thread @Async (não reativa).
     * .subscribe() fire-and-forget não garante envio antes do método retornar.
     */
    private void sendRaw(BotSession session, String text) {
        try {
            router.sendRaw(session.getResident(), session.getChannel(), text).block();
        } catch (Exception e) {
            log.error("Falha ao enviar mensagem pré-identificação residentId={}: {}",
                    session.getResident().getId(), e.getMessage(), e);
        }
    }
}
