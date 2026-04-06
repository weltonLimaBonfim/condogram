package com.condowhats.adapter.in.web;

import com.condowhats.adapter.in.web.dto.response.PageResponse;
import com.condowhats.adapter.in.web.dto.response.ReservationResponse;
import com.condowhats.service.reservation.ReservationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/condominiums/{condoId}/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Gestão de reservas de áreas comuns")
public class ReservationController {

    private final ReservationManagementService reservationService;

    @Operation(summary = "Listar reservas")
    @GetMapping
    public ResponseEntity<PageResponse<ReservationResponse>> list(
            @PathVariable Long condoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filtrar por data (yyyy-MM-dd)", example = "2025-06-20")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reservationService.list(condoId, date, page, size));
    }

    @Operation(summary = "Buscar reserva por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encontrada"),
            @ApiResponse(responseCode = "404", description = "Não encontrada", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> findById(
            @PathVariable Long condoId, @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.findById(condoId, id));
    }

    @Operation(summary = "Cancelar reserva")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancelada"),
            @ApiResponse(responseCode = "404", description = "Não encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Status não permite cancelamento", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ReservationResponse> cancel(
            @PathVariable Long condoId,
            @PathVariable Long id,
            @Parameter(example = "Solicitado pelo morador")
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(reservationService.cancel(condoId, id, reason));
    }
}
