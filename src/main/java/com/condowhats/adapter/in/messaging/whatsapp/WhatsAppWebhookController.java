package com.condowhats.adapter.in.messaging.whatsapp;

import com.condowhats.service.WebhookLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/whatsapp/webhook")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Webhook", description = "Endpoint chamado pela Meta Cloud API")
public class WhatsAppWebhookController {

    private final WhatsAppGatewayService gatewayService;
    private final WebhookLogService webhookLog;
    private final ObjectMapper objectMapper;

    @Value("${condowhats.whatsapp.app-secret:}")
    private String appSecret;

    @Value("${condowhats.whatsapp.verify-token:}")
    private String verifyToken;

    @Operation(summary = "Verificação do endpoint (Meta)", security = @SecurityRequirement(name = ""))
    @ApiResponse(responseCode = "200", description = "Token válido")
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WhatsApp webhook verificado");
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Forbidden");
    }

    @Operation(summary = "Receber mensagem (Meta)", security = @SecurityRequirement(name = ""))
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            HttpServletRequest request) {

        if (!isValidSignature(rawBody, signature)) {
            log.warn("HMAC inválido — IP: {}", request.getRemoteAddr());
            webhookLog.log(rawBody, Map.of("X-Hub-Signature-256", String.valueOf(signature)), false, null, "HMAC_INVALID");
            return ResponseEntity.status(401).build();
        }

        String errorMsg = null;
        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            String destination = payload
                    .at("/entry/0/changes/0/value/metadata/display_phone_number").asText();

            JsonNode messages = payload.at("/entry/0/changes/0/value/messages");
            if (!messages.isMissingNode() && messages.isArray() && !messages.isEmpty()) {
                gatewayService.processWebhook(payload, destination);
            }
        } catch (Exception e) {
            errorMsg = e.getMessage();
            log.error("Erro ao processar webhook WhatsApp", e);
        }

        webhookLog.log(rawBody, null, true, null, errorMsg);
        return ResponseEntity.ok().build();
    }

    private boolean isValidSignature(String body, String signature) {
        if (appSecret.isBlank() || signature == null || !signature.startsWith("sha256=")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
            return expected.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
