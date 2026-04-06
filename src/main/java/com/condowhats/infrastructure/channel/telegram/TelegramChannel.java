package com.condowhats.infrastructure.channel.telegram;

import com.condowhats.domain.model.ChannelConfig;
import com.condowhats.domain.port.Channel;
import com.condowhats.domain.port.MessagingChannel;
import com.condowhats.domain.port.OutboundMessage;
import com.condowhats.service.crypto.CredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramChannel implements MessagingChannel {

    private final WebClient telegramWebClient;
    private final CredentialService creds;

    @Value("${condowhats.telegram.api-base-url:https://api.telegram.org}")
    private String apiBaseUrl;

    @Override
    public Channel channel() {
        return Channel.TELEGRAM;
    }

    @Override
    public Mono<String> send(ChannelConfig config, OutboundMessage msg) {
        String token = creds.get(config, "botToken");
        String url = apiBaseUrl + "/bot" + token + "/sendMessage";
        return telegramWebClient.post()
                .uri(url)
                .bodyValue(buildPayload(msg))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(err -> Mono.error(new RuntimeException("Telegram error: " + err))))
                .bodyToMono(Map.class)
                .map(r -> String.valueOf(r.getOrDefault("result", "")))
                .doOnError(e -> log.error("Telegram send error [condo={}]: {}", config.getCondominium() != null ? config.getCondominium().getId() : "shared", e.getMessage()));
    }

    private Map<String, Object> buildPayload(OutboundMessage msg) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("chat_id", msg.recipientId());
        p.put("text", msg.text());
        p.put("parse_mode", "Markdown");
        if (msg.buttons() != null && !msg.buttons().isEmpty()) {
            List<List<Map<String, String>>> keyboard = msg.buttons().stream()
                    .map(row -> row.buttons().stream()
                            .map(btn -> Map.of("text", btn.label(), "callback_data", btn.callbackData()))
                            .toList())
                    .toList();
            p.put("reply_markup", Map.of("inline_keyboard", keyboard));
        }
        return p;
    }
}
