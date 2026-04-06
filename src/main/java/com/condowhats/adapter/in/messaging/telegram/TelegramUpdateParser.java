package com.condowhats.adapter.in.messaging.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Extrai dados relevantes de um Update do Telegram.
 * <p>
 * Dois tipos de update chegam:
 * - message:        texto enviado pelo usuário
 * - callback_query: clique em botão InlineKeyboard
 */
@Component
@Slf4j
public class TelegramUpdateParser {

    public Optional<ParsedUpdate> parse(JsonNode update) {
        try {
            if (update.has("message")) {
                return parseMessage(update.get("message"));
            }
            if (update.has("callback_query")) {
                return parseCallbackQuery(update.get("callback_query"));
            }
            log.debug("Update ignorado — tipo não suportado: {}", update.fieldNames().next());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Erro ao parsear update: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ParsedUpdate> parseMessage(JsonNode msg) {
        JsonNode from = msg.get("from");
        String chatId = msg.at("/chat/id").asText();
        String username = from.has("username") ? from.get("username").asText() : null;
        String firstName = from.has("first_name") ? from.get("first_name").asText("?") : "?";

        String type;
        String content;

        if (msg.has("text")) {
            type = "text";
            content = msg.get("text").asText();
        } else if (msg.has("photo")) {
            type = "photo";
            // Pega o file_id da maior resolução
            JsonNode photos = msg.get("photo");
            content = photos.get(photos.size() - 1).get("file_id").asText();
        } else if (msg.has("document")) {
            type = "document";
            content = msg.at("/document/file_id").asText();
        } else {
            type = "unknown";
            content = "";
        }

        return Optional.of(new ParsedUpdate(chatId, username, firstName, type, content, null));
    }

    private Optional<ParsedUpdate> parseCallbackQuery(JsonNode cb) {
        String chatId = cb.at("/message/chat/id").asText();
        String username = cb.at("/from/username").asText(null);
        String firstName = cb.at("/from/first_name").asText("?");
        String callbackQueryId = cb.get("id").asText();
        String data = cb.has("data") ? cb.get("data").asText() : "";

        return Optional.of(new ParsedUpdate(
                chatId, username, firstName, "callback_query", data, callbackQueryId
        ));
    }

    public record ParsedUpdate(
            String chatId,
            String username,
            String firstName,
            String messageType,    // "text" | "callback_query" | "photo" | "document" | "unknown"
            String content,        // texto ou callback_data
            String callbackQueryId // só em callback_query — necessário para answerCallbackQuery
    ) {
    }
}
