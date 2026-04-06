package com.condowhats.infrastructure.channel.whatsapp;

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

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppChannel implements MessagingChannel {

    private final WebClient telegramWebClient;
    private final CredentialService creds;

    @Value("${condowhats.whatsapp.api-base-url:https://graph.facebook.com/v19.0}")
    private String apiBaseUrl;

    @Override
    public Channel channel() {
        return Channel.WHATSAPP;
    }

    @Override
    public Mono<String> send(ChannelConfig config, OutboundMessage msg) {
        String phoneNumId = creds.get(config, "phoneNumberId");
        String accessToken = creds.get(config, "accessToken");
        return telegramWebClient.post()
                .uri(apiBaseUrl + "/" + phoneNumId + "/messages")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(buildPayload(msg))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(err -> Mono.error(new RuntimeException("WhatsApp error: " + err))))
                .bodyToMono(Map.class)
                .map(r -> String.valueOf(r.getOrDefault("messages", "")))
                .doOnError(e -> log.error("WhatsApp send error: {}", e.getMessage()));
    }

    private Map<String, Object> buildPayload(OutboundMessage msg) {
        if (msg.buttons() == null || msg.buttons().isEmpty()) {
            return Map.of("messaging_product", "whatsapp", "to", msg.recipientId(), "type", "text",
                    "text", Map.of("body", msg.text()));
        }
        List<OutboundMessage.Button> all = msg.buttons().stream()
                .flatMap(r -> r.buttons().stream()).toList();
        if (all.size() <= 3) {
            return Map.of("messaging_product", "whatsapp", "to", msg.recipientId(), "type", "interactive",
                    "interactive", Map.of("type", "button", "body", Map.of("text", msg.text()),
                            "action", Map.of("buttons", all.stream().map(b ->
                                    Map.of("type", "reply", "reply", Map.of("id", b.callbackData(), "title", truncate(b.label(), 20)))
                            ).toList())));
        }
        return Map.of("messaging_product", "whatsapp", "to", msg.recipientId(), "type", "interactive",
                "interactive", Map.of("type", "list", "body", Map.of("text", msg.text()),
                        "action", Map.of("button", "Ver opções", "sections", List.of(Map.of("title", "Opções",
                                "rows", all.stream().map(b -> Map.of("id", b.callbackData(), "title", truncate(b.label(), 24))).toList())))));
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
