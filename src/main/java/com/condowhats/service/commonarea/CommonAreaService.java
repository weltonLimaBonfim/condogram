package com.condowhats.service.commonarea;

import com.condowhats.adapter.in.web.dto.request.CreateCommonAreaRequest;
import com.condowhats.adapter.in.web.dto.response.CommonAreaResponse;
import com.condowhats.domain.model.CommonArea;
import com.condowhats.domain.repository.CommonAreaRepository;
import com.condowhats.domain.repository.CondominiumRepository;
import com.condowhats.exception.BusinessRuleException;
import com.condowhats.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonAreaService {

    private final CommonAreaRepository areaRepo;
    private final CondominiumRepository condoRepo;

    @Transactional(readOnly = true)
    public List<CommonAreaResponse> listActive(Long condoId) {
        assertCondominiumExists(condoId);
        return areaRepo.findByCondominiumIdAndActiveTrue(condoId).stream()
                .map(CommonAreaResponse::from)
                .toList();
    }

    @Transactional
    public CommonAreaResponse create(Long condoId, CreateCommonAreaRequest req) {
        var condo = condoRepo.findById(condoId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio", condoId));

        if (req.availableFrom().isAfter(req.availableUntil())) {
            throw new BusinessRuleException(
                    "Horário de início não pode ser posterior ao horário de término");
        }

        var area = areaRepo.save(CommonArea.builder()
                .condominium(condo)
                .name(req.name())
                .description(req.description())
                .capacity(req.capacity())
                .advanceDaysLimit(req.advanceDaysLimit() != null ? req.advanceDaysLimit() : 30)
                .maxDurationHours(req.maxDurationHours() != null ? req.maxDurationHours() : 4)
                .availableDays(req.availableDays())
                .availableFrom(req.availableFrom())
                .availableUntil(req.availableUntil())
                .requiresApproval(req.requiresApproval() != null ? req.requiresApproval() : false)
                .active(true)
                .build());

        log.info("Área comum criada: id={} name={} condo={}", area.getId(), area.getName(), condoId);
        return CommonAreaResponse.from(area);
    }

    @Transactional
    public void deactivate(Long condoId, Long id) {
        var area = areaRepo.findById(id)
                .filter(a -> a.getCondominium().getId().equals(condoId))
                .orElseThrow(() -> new ResourceNotFoundException("Área comum", id));

        if (!area.getActive()) {
            throw new BusinessRuleException("Área comum já está inativa");
        }

        area.setActive(false);
        areaRepo.save(area);
        log.info("Área comum desativada: id={} condo={}", id, condoId);
    }

    private void assertCondominiumExists(Long condoId) {
        if (!condoRepo.existsById(condoId)) {
            throw new ResourceNotFoundException("Condomínio", condoId);
        }
    }
}
