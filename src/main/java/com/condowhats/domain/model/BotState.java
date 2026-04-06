package com.condowhats.domain.model;

public enum BotState {
    // ── Identificação (bot único) ──────────────────────────────────────────
    IDENTIFYING,          // aguardando CPF
    CPF_NOT_FOUND,        // CPF não cadastrado — aguarda nova tentativa ou suporte
    CPF_BLOCKED,          // morador bloqueado — encerra

    // ── Menu principal ─────────────────────────────────────────────────────
    MAIN_MENU,

    // ── Ocorrências ────────────────────────────────────────────────────────
    OCCURRENCE_TITLE,
    OCCURRENCE_CATEGORY,
    OCCURRENCE_DESC,
    OCCURRENCE_CONFIRM,

    // ── Reservas ──────────────────────────────────────────────────────────
    RESERVATION_AREA,
    RESERVATION_DATE,
    RESERVATION_TIME,
    RESERVATION_CONFIRM,

    // ── Outros ────────────────────────────────────────────────────────────
    NOTIFICATION_ACK,
    AWAITING_HUMAN,

    // Estado legado — alias de IDENTIFYING para retrocompatibilidade
    IDLE;

    /**
     * Sessão precisa de identificação?
     */
    public boolean needsIdentification() {
        return this == IDLE || this == IDENTIFYING || this == CPF_NOT_FOUND;
    }
}
