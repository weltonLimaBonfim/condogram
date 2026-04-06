package com.condowhats.service.resident;

import com.condowhats.adapter.in.web.dto.request.CreateResidentRequest;
import com.condowhats.adapter.in.web.dto.request.LinkResidentChannelRequest;
import com.condowhats.adapter.in.web.dto.response.PageResponse;
import com.condowhats.adapter.in.web.dto.response.ResidentResponse;
import com.condowhats.domain.model.Resident;
import com.condowhats.domain.model.ResidentChannel;
import com.condowhats.domain.repository.CondominiumRepository;
import com.condowhats.domain.repository.ResidentChannelRepository;
import com.condowhats.domain.repository.ResidentRepository;
import com.condowhats.exception.BusinessRuleException;
import com.condowhats.exception.ConflictException;
import com.condowhats.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentRepository residentRepo;
    private final CondominiumRepository condoRepo;
    private final ResidentChannelRepository residentChannelRepo;

    @Transactional(readOnly = true)
    public PageResponse<ResidentResponse> list(Long condoId, String status, int page, int size) {
        if (!condoRepo.existsById(condoId)) throw new ResourceNotFoundException("Condomínio", condoId);
        var pageable = PageRequest.of(page, size, Sort.by("name"));
        var result = status != null
                ? residentRepo.findByCondominiumIdAndStatus(condoId, parseStatus(status), pageable)
                : residentRepo.findByCondominiumId(condoId, pageable);
        return PageResponse.from(result.map(ResidentResponse::from));
    }

    @Transactional(readOnly = true)
    public ResidentResponse findById(Long condoId, Long id) {
        return residentRepo.findByIdAndCondominiumId(id, condoId)
                .map(ResidentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Morador", id));
    }

    @Transactional
    public ResidentResponse create(Long condoId, CreateResidentRequest req) {
        var condo = condoRepo.findById(condoId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio", condoId));

        String cpf = Resident.normalizeCpf(req.cpf());
        if (residentRepo.findByCondominiumIdAndCpf(condoId, cpf).isPresent()) {
            throw new ConflictException("CPF já cadastrado neste condomínio: " + cpf);
        }

        var resident = residentRepo.save(Resident.builder()
                .condominium(condo)
                .name(req.name())
                .cpf(cpf)
                .unitNumber(req.unitNumber())
                .block(req.block())
                .role(resolveRole(req.role()))
                .status(Resident.ResidentStatus.ACTIVE)
                .build());

        log.info("Morador cadastrado: id={} cpf={}*** condo={}", resident.getId(),
                cpf.substring(0, 3), condoId);
        return ResidentResponse.from(resident);
    }

    @Transactional
    public ResidentResponse block(Long condoId, Long id) {
        var resident = residentRepo.findByIdAndCondominiumId(id, condoId)
                .orElseThrow(() -> new ResourceNotFoundException("Morador", id));
        if (resident.getStatus() == Resident.ResidentStatus.BLOCKED)
            throw new BusinessRuleException("Morador já está bloqueado");
        resident.setStatus(Resident.ResidentStatus.BLOCKED);
        return ResidentResponse.from(residentRepo.save(resident));
    }

    @Transactional
    public void linkChannel(Long condoId, Long residentId, LinkResidentChannelRequest req) {
        var resident = residentRepo.findByIdAndCondominiumId(residentId, condoId)
                .orElseThrow(() -> new ResourceNotFoundException("Morador", residentId));

        residentChannelRepo.findByExternalIdAndChannel(req.externalId(), req.channel())
                .filter(rc -> !rc.getResident().getId().equals(residentId))
                .ifPresent(rc -> {
                    throw new ConflictException(
                            "ID externo já vinculado a outro morador: " + req.externalId());
                });

        var existing = residentChannelRepo.findByResidentIdAndChannel(residentId, req.channel());
        if (existing.isPresent()) {
            var rc = existing.get();
            rc.setExternalId(req.externalId());
            rc.setDisplayHandle(req.displayHandle());
            residentChannelRepo.save(rc);
        } else {
            residentChannelRepo.save(ResidentChannel.builder()
                    .resident(resident)
                    .channel(req.channel())
                    .externalId(req.externalId())
                    .displayHandle(req.displayHandle())
                    .optedIn(false)
                    .build());
        }
    }

    private Resident.ResidentStatus parseStatus(String raw) {
        try {
            return Resident.ResidentStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Status inválido: " + raw);
        }
    }

    private Resident.ResidentRole resolveRole(String raw) {
        try {
            return raw != null ? Resident.ResidentRole.valueOf(raw.toUpperCase()) : Resident.ResidentRole.RESIDENT;
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Role inválida: " + raw);
        }
    }
}
