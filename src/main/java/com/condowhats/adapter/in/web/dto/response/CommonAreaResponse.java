package com.condowhats.adapter.in.web.dto.response;

import com.condowhats.domain.model.CommonArea;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.List;

@Schema(description = "Dados de uma área comum")
public record CommonAreaResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "Salão de Festas") String name,
        @Schema(example = "Salão com cozinha equipada") String description,
        @Schema(example = "80") Integer capacity,
        @Schema(example = "08:00") LocalTime availableFrom,
        @Schema(example = "22:00") LocalTime availableUntil,
        List<String> availableDays,
        @Schema(example = "false") Boolean requiresApproval,
        @Schema(example = "true") Boolean active
) {
    public static CommonAreaResponse from(CommonArea a) {
        return new CommonAreaResponse(
                a.getId(), a.getName(), a.getDescription(), a.getCapacity(),
                a.getAvailableFrom(), a.getAvailableUntil(),
                a.getAvailableDays(), a.getRequiresApproval(), a.getActive()
        );
    }
}
