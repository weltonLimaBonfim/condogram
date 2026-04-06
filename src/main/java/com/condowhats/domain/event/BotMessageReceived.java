package com.condowhats.domain.event;

import com.condowhats.domain.port.Channel;

import java.time.Instant;

/**
 * Evento publicado por qualquer gateway ao receber mensagem de um morador.
 * Totalmente agnóstico de canal — o BotOrchestratorService não precisa
 * saber se veio do Telegram, WhatsApp ou qualquer futuro canal.
 */
public record BotMessageReceived(
        Long condominiumId,
        Long residentId,
        Long sessionId,
        Channel channel,            // qual canal originou a mensagem
        String externalMessageId,
        String messageType,        // text | callback_query | button | image
        String content,            // texto digitado ou callback_data do botão
        String rawPayload,
        Instant occurredAt
) {
}
