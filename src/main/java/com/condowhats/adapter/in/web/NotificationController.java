package com.condowhats.adapter.in.web;

import com.condowhats.adapter.in.web.dto.request.SendAnnouncementRequest;
import com.condowhats.service.notification.NotificationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/condominiums/{condoId}/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificações", description = "Envio de comunicados proativos via Telegram")
public class NotificationController {

    private final NotificationManagementService notificationService;

    @Operation(
            summary = "Enviar comunicado geral",
            description = "Envia template `cw_announcement` para todos os moradores com opt-in. Envio assíncrono."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Envio iniciado"),
            @ApiResponse(responseCode = "404", description = "Condomínio não encontrado", content = @Content)
    })
    @PostMapping("/announcement")
    public ResponseEntity<Map<String, String>> sendAnnouncement(
            @PathVariable Long condoId,
            @Valid @RequestBody SendAnnouncementRequest req) {
        notificationService.sendAnnouncement(condoId, req.subject(), req.body());
        return ResponseEntity.accepted()
                .body(Map.of("status", "processing",
                        "message", "Comunicado sendo enviado para os moradores com opt-in"));
    }
}
