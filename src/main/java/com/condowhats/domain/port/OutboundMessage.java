package com.condowhats.domain.port;

import java.util.List;

/**
 * Mensagem de saída normalizada — o canal traduz para o formato nativo.
 *
 * @param recipientId ID do destinatário no canal (chat_id do Telegram, phone E.164 no WhatsApp)
 * @param text        Texto em Markdown simples (*bold*, _italic_) — o canal converte para MarkdownV2 ou WhatsApp fmt
 * @param buttons     Botões de ação (null = sem botões)
 */
public record OutboundMessage(
        String recipientId,
        String text,
        List<ButtonRow> buttons
) {
    public static OutboundMessage text(String recipientId, String text) {
        return new OutboundMessage(recipientId, text, null);
    }

    public static OutboundMessage withButtons(String recipientId, String text, List<ButtonRow> rows) {
        return new OutboundMessage(recipientId, text, rows);
    }

    // ── Factories ─────────────────────────────────────────────────────────────

    /**
     * Atalho para uma única linha de botões
     */
    public static OutboundMessage singleRow(String recipientId, String text, Button... buttons) {
        return new OutboundMessage(recipientId, text, List.of(new ButtonRow(List.of(buttons))));
    }

    /**
     * Uma linha de botões horizontais
     */
    public record ButtonRow(List<Button> buttons) {
    }

    /**
     * Um botão individual
     */
    public record Button(String label, String callbackData) {
    }
}
