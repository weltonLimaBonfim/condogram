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

/**
 * Gerencia o fluxo de identificação por CPF no bot único multicondomínio.
 * <p>
 * Estados:
 * IDENTIFYING      → aguarda CPF
 * CPF_NOT_FOUND    → CPF não encontrado após MAX_ATTEMPTS tentativas
 * CPF_BLOCKED      → morador está bloqueado (enceramento)
 * MAIN_MENU        → identificação concluída com sucesso
 */
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

    /**
     * Processa o texto enviado como CPF.
     * Retorna true se a identificação foi concluída, false se ainda pendente.
     */
    @Transactional
    public boolean handleCpfInput(BotSession session, String input) {
        String cpf = Resident.normalizeCpf(input);

        if (cpf == null || cpf.length() != 11 || !cpf.matches("\\d{11}")) {
            sendRaw(session, "CPF inválido. Digite apenas os 11 números.\nExemplo: 12345678901");
            return false;
        }

        List<Resident> found = residentRepo.findAllByCpfAndActive(cpf);

        if (found.isEmpty()) {
            return handleNotFound(session, cpf);
        }
        if (found.size() == 1) {
            return identifyAs(session, found.get(0));
        }
        return handleMultipleCondos(session, found);
    }

    /**
     * Processa a escolha do condomínio quando o morador está em múltiplos.
     * Retorna true se a identificação foi concluída.
     */
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

        // Usa sendRaw porque session ainda não tem condomínio definido
        router.sendRawWithButtons(session.getResident(), session.getChannel(),
                "Seu CPF está cadastrado em mais de um condomínio. Selecione:", rows
        ).subscribe();

        return false;
    }

    /**
     * Conclui a identificação:
     * 1. Migra o ResidentChannel do placeholder para o Resident real
     * 2. Deleta o placeholder (após desvincular o canal)
     * 3. Vincula a sessão ao condomínio correto
     * 4. Envia boas-vindas
     */
    @Transactional
    boolean identifyAs(BotSession session, Resident identified) {
        Resident placeholder = session.getResident();

        if (!placeholder.getId().equals(identified.getId())) {
            // 1. Migra o ResidentChannel PRIMEIRO (antes de deletar o placeholder)
            residentChannelRepo
                    .findByResidentIdAndChannel(placeholder.getId(), session.getChannel())
                    .ifPresent(rc -> {
                        rc.setResident(identified);   // aponta para o residente real
                        rc.setOptedIn(true);
                        rc.setOptedInAt(Instant.now());
                        residentChannelRepo.save(rc);
                        residentChannelRepo.flush();   // garante que a FK foi atualizada no banco
                    });

            // 2. Atualiza a sessão para o residente real
            session.setResident(identified);
            sessionRepo.save(session);

            // 3. Deleta o placeholder — agora sem FK pendente
            residentRepo.delete(placeholder);
            residentRepo.flush();

        } else {
            // Morador já identificado retornando com sessão expirada
            residentChannelRepo
                    .findByResidentIdAndChannel(identified.getId(), session.getChannel())
                    .ifPresent(rc -> {
                        if (!rc.getOptedIn()) {
                            rc.setOptedIn(true);
                            rc.setOptedInAt(Instant.now());
                            residentChannelRepo.save(rc);
                        }
                    });
        }

        // 4. Vincula sessão ao condomínio e avança o estado
        session.setCondominium(identified.getCondominium());
        session.setBotState(BotState.MAIN_MENU);
        session.getContextData().clear();
        sessionRepo.save(session);

        // 5. Boas-vindas com nome, condomínio e unidade
        String greeting = String.format(
                "Olá, *%s*! Bem-vindo ao *%s* \uD83C\uDFE2\n\nApto: %s%s",
                identified.getName(),
                identified.getCondominium().getName(),
                identified.getUnitNumber(),
                identified.getBlock() != null ? " — Bloco " + identified.getBlock() : ""
        );
        // Já tem condomínio — pode usar send normal
        router.send(
                identified.getCondominium(), identified, session.getChannel(), greeting
        ).subscribe();

        log.info("Morador identificado: residentId={} condo={}",
                identified.getId(), identified.getCondominium().getId());
        return true;
    }

    // ── Envio pré-identificação (sem condomínio na sessão) ────────────────────

    /**
     * Envia texto antes da identificação — usa o bot shared via sendRaw.
     * NÃO usa router.send() porque session.getCondominium() ainda é null.
     */
    private void sendRaw(BotSession session, String text) {
        router.sendRaw(session.getResident(), session.getChannel(), text).subscribe();
    }
}
