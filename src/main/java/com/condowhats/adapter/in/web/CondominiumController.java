package com.condowhats.adapter.in.web;

import com.condowhats.adapter.in.web.dto.request.AddChannelConfigRequest;
import com.condowhats.adapter.in.web.dto.request.CreateCondominiumRequest;
import com.condowhats.adapter.in.web.dto.response.CondominiumResponse;
import com.condowhats.adapter.in.web.dto.response.PageResponse;
import com.condowhats.service.condominium.CondominiumService;
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

@RestController
@RequestMapping("/api/v1/condominiums")
@RequiredArgsConstructor
@Tag(name = "Condomínios", description = "Gestão de condomínios e seus canais de mensageria")
public class CondominiumController {

    private final CondominiumService condominiumService;

    @Operation(summary = "Listar condomínios")
    @GetMapping
    public ResponseEntity<PageResponse<CondominiumResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(condominiumService.listAll(page, size));
    }

    @Operation(summary = "Buscar condomínio por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encontrado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<CondominiumResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(condominiumService.findById(id));
    }

    @Operation(summary = "Cadastrar condomínio")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Criado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CondominiumResponse> create(@Valid @RequestBody CreateCondominiumRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(condominiumService.create(req));
    }

    @Operation(summary = "Ativar condomínio")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<CondominiumResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(condominiumService.activate(id));
    }

    @Operation(
            summary = "Configurar canal de mensageria",
            description = """
                    Adiciona ou atualiza um canal (Telegram ou WhatsApp) para o condomínio.
                    
                    **Telegram** — credentials:
                    ```json
                    { "botToken": "7123:AAH...", "webhookSecret": "seu-secret" }
                    ```
                    publicIdentifier = username do bot sem @ (ex: `residencial_flores_bot`)
                    
                    **WhatsApp** — credentials:
                    ```json
                    { "phoneNumberId": "123...", "accessToken": "EAA...", "appSecret": "abc..." }
                    ```
                    publicIdentifier = número E.164 (ex: `+5511999990000`)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Canal configurado"),
            @ApiResponse(responseCode = "404", description = "Condomínio não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Identificador já em uso", content = @Content)
    })
    @PutMapping("/{id}/channels")
    public ResponseEntity<Void> addChannel(
            @PathVariable Long id,
            @Valid @RequestBody AddChannelConfigRequest req) {
        condominiumService.addChannelConfig(id, req);
        return ResponseEntity.noContent().build();
    }
}
