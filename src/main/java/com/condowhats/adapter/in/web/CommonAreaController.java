package com.condowhats.adapter.in.web;

import com.condowhats.adapter.in.web.dto.request.CreateCommonAreaRequest;
import com.condowhats.adapter.in.web.dto.response.CommonAreaResponse;
import com.condowhats.service.commonarea.CommonAreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/condominiums/{condoId}/areas")
@RequiredArgsConstructor
@Tag(name = "Áreas Comuns", description = "Gestão das áreas comuns disponíveis para reserva")
public class CommonAreaController {

    private final CommonAreaService commonAreaService;

    @Operation(summary = "Listar áreas ativas")
    @GetMapping
    public ResponseEntity<List<CommonAreaResponse>> list(@PathVariable Long condoId) {
        return ResponseEntity.ok(commonAreaService.listActive(condoId));
    }

    @Operation(summary = "Cadastrar área comum")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "422", description = "Horário inválido", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CommonAreaResponse> create(
            @PathVariable Long condoId,
            @Valid @RequestBody CreateCommonAreaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commonAreaService.create(condoId, req));
    }

    @Operation(summary = "Desativar área comum")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Desativada"),
            @ApiResponse(responseCode = "404", description = "Não encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Já está inativa", content = @Content)
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long condoId, @PathVariable Long id) {
        commonAreaService.deactivate(condoId, id);
        return ResponseEntity.noContent().build();
    }
}
