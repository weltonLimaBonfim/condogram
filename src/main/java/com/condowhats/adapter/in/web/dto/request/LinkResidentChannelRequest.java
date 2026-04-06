package com.condowhats.adapter.in.web.dto.request;

import com.condowhats.domain.port.Channel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Vincula um morador a um canal de mensageria")
public record LinkResidentChannelRequest(

        @Schema(example = "TELEGRAM", allowableValues = {"TELEGRAM", "WHATSAPP"})
        @NotNull
        Channel channel,

        @Schema(
                description = "ID externo no canal: chat_id numérico (Telegram) ou número E.164 (WhatsApp)",
                example = "123456789"
        )
        @NotBlank
        String externalId,

        @Schema(description = "Handle/username no canal (opcional)", example = "joaosilva")
        String displayHandle
) {
}
