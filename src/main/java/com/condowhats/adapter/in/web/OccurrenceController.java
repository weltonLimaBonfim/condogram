package com.condowhats.adapter.in.web;

import com.condowhats.adapter.in.web.dto.request.UpdateOccurrenceRequest;
import com.condowhats.adapter.in.web.dto.response.OccurrenceResponse;
import com.condowhats.adapter.in.web.dto.response.PageResponse;
import com.condowhats.service.occurrence.OccurrenceManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/condominiums/{condoId}/occurrences")
@RequiredArgsConstructor
@Tag(name = "Ocorrências", description = "Gestão de ocorrências e chamados do condomínio")
public class OccurrenceController {

    private final OccurrenceManagementService occurrenceService;

    @Operation(summary = "Listar ocorrências")
    @GetMapping
    public ResponseEntity<PageResponse<OccurrenceResponse>> list(
            @PathVariable Long condoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(occurrenceService.list(condoId, page, size));
    }

    @Operation(summary = "Buscar ocorrência por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encontrada"),
            @ApiResponse(responseCode = "404", description = "Não encontrada", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<OccurrenceResponse> findById(
            @PathVariable Long condoId, @PathVariable Long id) {
        return ResponseEntity.ok(occurrenceService.findById(condoId, id));
    }

    @Operation(summary = "Buscar por protocolo")
    @GetMapping("/ticket/{ticket}")
    public ResponseEntity<OccurrenceResponse> findByTicket(
            @PathVariable Long condoId,
            @Parameter(example = "OC-2025-00007") @PathVariable String ticket) {
        return ResponseEntity.ok(occurrenceService.findByTicket(condoId, ticket));
    }

    @Operation(
            summary = "Atualizar ocorrência",
            description = "Atualiza status e, se `notifyResident=true`, notifica o morador via Telegram"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atualizada"),
            @ApiResponse(responseCode = "404", description = "Não encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Transição de status inválida", content = @Content)
    })
    @PatchMapping("/{id}")
    public ResponseEntity<OccurrenceResponse> update(
            @PathVariable Long condoId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOccurrenceRequest req) {
        return ResponseEntity.ok(occurrenceService.update(condoId, id, req));
    }
}
