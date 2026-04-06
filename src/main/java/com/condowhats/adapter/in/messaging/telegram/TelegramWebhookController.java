package com.condowhats.adapter.in.messaging.telegram;

import com.condowhats.adapter.in.messaging.telegram.TelegramGatewayService;
import com.condowhats.service.WebhookLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Recebe updates do Telegram via Webhook.
 * <p>
 * URL: POST /telegram/webhook/{botUsername}
 * <p>
 * O Telegram envia os updates para a URL configurada via setWebhook.
 * Cada bot de cada condomínio tem seu próprio token, mas todos usam
 * o mesmo endpoint — o botUsername na URL identifica o condomínio.
 * <p>
 * Autenticação: o Telegram envia o header X-Telegram-Bot-Api-Secret-Token
 * com o valor configurado em setWebhook — validamos aqui.
 */
@Slf4j
@RestController
@RequestMapping("/telegram/webhook")
@RequiredArgsConstructor
@Tag(name = "Telegram Webhook", description = "Endpoint chamado pelo Telegram — não chamar manualmente")
public class TelegramWebhookController {

    private final TelegramGatewayService gatewayService;
    private final WebhookLogService webhookLog;
    private final ObjectMapper objectMapper;

    @Value("${condowhats.telegram.webhook-secret}")
    private String webhookSecret;

    @Operation(
            summary = "Receber update do Telegram",
            description = """
                    Recebe updates (mensagens e callback_queries) do Telegram.
                    O header `X-Telegram-Bot-Api-Secret-Token` valida a origem.
                    O processamento é assíncrono — retorna 200 imediatamente.
                    """,
            security = @SecurityRequirement(name = "")
    )
    @ApiResponse(responseCode = "200", description = "Update recebido")
    @ApiResponse(responseCode = "403", description = "Secret token inválido")
    @PostMapping("/{botUsername}")
    public ResponseEntity<Void> receive(
            @PathVariable String botUsername,
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret) {

        // Valida o secret token configurado no setWebhook
        if (!webhookSecret.equals(secret)) {
            log.warn("Secret token inválido no webhook Telegram | bot=@{}", botUsername);
            return ResponseEntity.status(403).build();
        }

        String errorMsg = null;
        try {
            JsonNode update = objectMapper.readTree(rawBody);
            gatewayService.processUpdate(update, botUsername);
        } catch (Exception e) {
            errorMsg = e.getMessage();
            log.error("Erro ao processar update Telegram | bot=@{}: {}", botUsername, e.getMessage());
        }

        // Telegram exige 200 imediato — processamento é assíncrono
        webhookLog.log(rawBody, Map.of("bot", botUsername), true, null, errorMsg);
        return ResponseEntity.ok().build();
    }
}
