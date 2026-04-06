package com.condowhats.adapter.in.web.dto.request;

import com.condowhats.domain.port.Channel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Configura um canal de mensageria")
public record AddChannelConfigRequest(

        @Schema(example = "TELEGRAM", allowableValues = {"TELEGRAM", "WHATSAPP"})
        @NotNull
        Channel channel,

        @Schema(description = """
                Credenciais do canal:
                - TELEGRAM: { "botToken": "...", "webhookSecret": "..." }
                - WHATSAPP: { "phoneNumberId": "...", "accessToken": "...", "appSecret": "..." }
                """)
        @NotNull
        Map<String, String> credentials,

        @Schema(description = "Username do bot (Telegram) ou número E.164 (WhatsApp)",
                example = "condowhats_bot")
        @NotBlank
        String publicIdentifier,

        @Schema(description = "true = bot único para todos os condomínios (identificação por CPF). false = bot exclusivo deste condomínio.",
                example = "true")
        boolean sharedBot
) {
}
