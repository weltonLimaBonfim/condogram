package com.condowhats.adapter.in.messaging.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades lidas de condowhats.telegram.* no application.yml.
 * <p>
 * O Telegram suporta dois modos de recebimento de updates:
 * - Polling: a aplicação consulta a API periodicamente (simples, sem infra extra)
 * - Webhook: o Telegram faz POST para a sua URL (requer HTTPS público)
 * <p>
 * Para MVP use polling. Para produção use webhook.
 */
@ConfigurationProperties(prefix = "condowhats.telegram")
public record TelegramProperties(
        String apiBaseUrl,      // https://api.telegram.org
        String webhookSecret,   // header X-Telegram-Bot-Api-Secret-Token
        String webhookPath,     // /telegram/webhook
        Mode mode             // POLLING | WEBHOOK
) {
    public enum Mode {POLLING, WEBHOOK}
}
