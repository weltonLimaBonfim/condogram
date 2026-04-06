package com.condowhats.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

@Schema(description = "Dados para cadastro de área comum")
public record CreateCommonAreaRequest(

        @Schema(description = "Nome da área", example = "Salão de Festas")
        @NotBlank
        String name,

        @Schema(description = "Descrição detalhada", example = "Salão com capacidade para 80 pessoas, cozinha equipada")
        String description,

        @Schema(description = "Capacidade máxima de pessoas", example = "80")
        @NotNull @Min(1)
        Integer capacity,

        @Schema(description = "Antecedência máxima para reserva (dias)", example = "30")
        Integer advanceDaysLimit,

        @Schema(description = "Duração máxima da reserva (horas)", example = "4")
        Integer maxDurationHours,

        @Schema(description = "Dias disponíveis para reserva", example = "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\",\"SAT\",\"SUN\"]")
        @NotNull
        List<String> availableDays,

        @Schema(description = "Horário de início de disponibilidade", example = "08:00")
        @NotNull
        LocalTime availableFrom,

        @Schema(description = "Horário de fim de disponibilidade", example = "22:00")
        @NotNull
        LocalTime availableUntil,

        @Schema(description = "Se true, reservas precisam de aprovação do síndico", example = "false")
        Boolean requiresApproval
) {
}
