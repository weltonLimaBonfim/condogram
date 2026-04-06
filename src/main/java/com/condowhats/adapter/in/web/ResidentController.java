package com.condowhats.adapter.in.web;

import com.condowhats.adapter.in.web.dto.request.CreateResidentRequest;
import com.condowhats.adapter.in.web.dto.request.LinkResidentChannelRequest;
import com.condowhats.adapter.in.web.dto.response.PageResponse;
import com.condowhats.adapter.in.web.dto.response.ResidentResponse;
import com.condowhats.service.resident.ResidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/condominiums/{condoId}/residents")
@RequiredArgsConstructor
@Tag(name = "Moradores", description = "Gestão de moradores e seus canais de comunicação")
public class ResidentController {

    private final ResidentService residentService;

    @Operation(summary = "Listar moradores")
    @GetMapping
    public ResponseEntity<PageResponse<ResidentResponse>> list(
            @PathVariable Long condoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "ACTIVE | INACTIVE | BLOCKED")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(residentService.list(condoId, status, page, size));
    }

    @Operation(summary = "Buscar morador por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encontrado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResidentResponse> findById(@PathVariable Long condoId, @PathVariable Long id) {
        return ResponseEntity.ok(residentService.findById(condoId, id));
    }

    @Operation(summary = "Pré-cadastrar morador",
            description = "Cadastra o morador sem canal. Vincule canais via PUT /{id}/channels.")
    @PostMapping
    public ResponseEntity<ResidentResponse> create(
            @PathVariable Long condoId,
            @Valid @RequestBody CreateResidentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(residentService.create(condoId, req));
    }

    @Operation(summary = "Bloquear morador")
    @PatchMapping("/{id}/block")
    public ResponseEntity<ResidentResponse> block(@PathVariable Long condoId, @PathVariable Long id) {
        return ResponseEntity.ok(residentService.block(condoId, id));
    }

    @Operation(
            summary = "Vincular canal ao morador",
            description = """
                    Associa um externalId de canal ao morador.
                    
                    - **Telegram**: externalId = chat_id numérico (obtido quando o morador manda /start ao bot)
                    - **WhatsApp**: externalId = número E.164 (+5511988887777)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Canal vinculado"),
            @ApiResponse(responseCode = "409", description = "ID externo já vinculado a outro morador", content = @Content)
    })
    @PutMapping("/{id}/channels")
    public ResponseEntity<Void> linkChannel(
            @PathVariable Long condoId,
            @PathVariable Long id,
            @Valid @RequestBody LinkResidentChannelRequest req) {
        residentService.linkChannel(condoId, id, req);
        return ResponseEntity.noContent().build();
    }
}
