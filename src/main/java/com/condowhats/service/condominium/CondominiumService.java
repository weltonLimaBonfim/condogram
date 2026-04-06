package com.condowhats.service.condominium;

import com.condowhats.adapter.in.web.dto.request.AddChannelConfigRequest;
import com.condowhats.adapter.in.web.dto.request.CreateCondominiumRequest;
import com.condowhats.adapter.in.web.dto.response.CondominiumResponse;
import com.condowhats.adapter.in.web.dto.response.PageResponse;
import com.condowhats.domain.model.ChannelConfig;
import com.condowhats.domain.model.Condominium;
import com.condowhats.domain.repository.ChannelConfigRepository;
import com.condowhats.domain.repository.CondominiumRepository;
import com.condowhats.domain.repository.ManagementCompanyRepository;
import com.condowhats.exception.BusinessRuleException;
import com.condowhats.exception.ConflictException;
import com.condowhats.exception.ResourceNotFoundException;
import com.condowhats.service.crypto.CredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CondominiumService {

    private final CondominiumRepository condominiumRepo;
    private final ManagementCompanyRepository managementCompanyRepo;
    private final ChannelConfigRepository channelConfigRepo;
    private final CredentialService credentialService;

    @Transactional(readOnly = true)
    public PageResponse<CondominiumResponse> listAll(int page, int size) {
        return PageResponse.from(
                condominiumRepo.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                        .map(CondominiumResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public CondominiumResponse findById(Long id) {
        return condominiumRepo.findById(id)
                .map(CondominiumResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio", id));
    }

    @Transactional
    public CondominiumResponse create(CreateCondominiumRequest req) {
        var mgmt = req.managementCompanyId() != null
                ? managementCompanyRepo.findById(req.managementCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Administradora", req.managementCompanyId()))
                : managementCompanyRepo.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Administradora", "nenhuma cadastrada"));

        var condo = condominiumRepo.save(Condominium.builder()
                .managementCompany(mgmt)
                .name(req.name()).cnpj(req.cnpj())
                .address(req.address()).city(req.city()).state(req.state()).zipCode(req.zipCode())
                .botGreeting(req.botGreeting())
                .status(Condominium.CondoStatus.SETUP)
                .build());

        log.info("Condomínio criado: id={}", condo.getId());
        return CondominiumResponse.from(condo);
    }

    @Transactional
    public CondominiumResponse activate(Long id) {
        var condo = condominiumRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio", id));
        if (condo.getStatus() == Condominium.CondoStatus.ACTIVE)
            throw new BusinessRuleException("Condomínio já está ativo");
        condo.setStatus(Condominium.CondoStatus.ACTIVE);
        return CondominiumResponse.from(condominiumRepo.save(condo));
    }

    /**
     * Configura um canal para o condomínio (ou bot compartilhado quando sharedBot=true).
     * <p>
     * Bot compartilhado: condominiumId pode ser null — o canal serve todos os condomínios.
     * O morador é identificado via CPF no início da conversa.
     */
    @Transactional
    public void addChannelConfig(Long condoId, AddChannelConfigRequest req) {
        Condominium condo = condoId != null
                ? condominiumRepo.findById(condoId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio", condoId))
                : null;

        // Verifica conflito de publicIdentifier
        channelConfigRepo.findByPublicIdentifierAndChannel(req.publicIdentifier(), req.channel())
                .filter(cfg -> !isOwnConfig(cfg, condoId))
                .ifPresent(cfg -> {
                    throw new ConflictException(
                            "Identificador já em uso: " + req.publicIdentifier());
                });

        // Se é shared bot, só pode haver um por canal
        if (req.sharedBot()) {
            channelConfigRepo.findByChannelAndSharedBotTrueAndActiveTrue(req.channel())
                    .filter(cfg -> !cfg.getPublicIdentifier().equals(req.publicIdentifier()))
                    .ifPresent(cfg -> {
                        throw new ConflictException(
                                "Já existe um bot compartilhado para o canal " + req.channel() +
                                        " (@" + cfg.getPublicIdentifier() + "). Desative-o primeiro.");
                    });
        }

        String encCredentials = credentialService.encrypt(req.credentials());

        Optional<ChannelConfig> existing;
        if (req.sharedBot()) {
            existing = channelConfigRepo.findByChannelAndSharedBotTrueAndActiveTrue(req.channel());
        } else if (condoId != null) {
            existing = channelConfigRepo.findByCondominiumIdAndChannelAndActiveTrue(condoId, req.channel());
        } else {
            existing = Optional.empty();
        }

        if (existing.isPresent()) {
            var cfg = existing.get();
            cfg.setCredentialsJsonEnc(encCredentials);
            cfg.setPublicIdentifier(req.publicIdentifier());
            channelConfigRepo.save(cfg);
            log.info("ChannelConfig atualizado: canal={} shared={}", req.channel(), req.sharedBot());
        } else {
            channelConfigRepo.save(ChannelConfig.builder()
                    .condominium(condo)
                    .channel(req.channel())
                    .sharedBot(req.sharedBot())
                    .credentialsJsonEnc(encCredentials)
                    .publicIdentifier(req.publicIdentifier())
                    .active(true)
                    .build());
            log.info("ChannelConfig criado: canal={} shared={}", req.channel(), req.sharedBot());
        }
    }

    private boolean isOwnConfig(ChannelConfig cfg, Long condoId) {
        if (cfg.getSharedBot()) return false;
        if (condoId == null) return false;
        return cfg.getCondominium() != null && cfg.getCondominium().getId().equals(condoId);
    }
}
