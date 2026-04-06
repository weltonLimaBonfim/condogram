package com.condowhats.adapter.in.messaging.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class WhatsAppUpdateParser {

    public Optional<ParsedUpdate> parse(JsonNode payload) {
        try {
            JsonNode msg = payload.at("/entry/0/changes/0/value/messages/0");
            if (msg.isMissingNode()) return Optional.empty();
            return Optional.of(new ParsedUpdate(
                    msg.get("from").asText(),
                    msg.get("id").asText(),
                    msg.get("type").asText(),
                    extractContent(msg, msg.get("type").asText())
            ));
        } catch (Exception e) {
            log.warn("Erro ao parsear payload WhatsApp: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String extractContent(JsonNode msg, String type) {
        return switch (type) {
            case "text" -> msg.at("/text/body").asText();
            case "button" -> msg.at("/button/payload").asText();
            case "list_reply" -> msg.at("/interactive/list_reply/id").asText();
            case "image" -> msg.at("/image/id").asText();
            default -> type;
        };
    }

    public record ParsedUpdate(
            String fromPhone,
            String messageId,
            String messageType,
            String content
    ) {
    }
}
