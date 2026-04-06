package com.condowhats.service.auth;

import com.condowhats.adapter.in.web.dto.request.CreateUserRequest;
import com.condowhats.adapter.in.web.dto.request.LoginRequest;
import com.condowhats.adapter.in.web.dto.request.RefreshTokenRequest;
import com.condowhats.adapter.in.web.dto.response.AuthResponse;
import com.condowhats.adapter.in.web.dto.response.UserResponse;
import com.condowhats.domain.model.AppUser;
import com.condowhats.domain.repository.AppUserRepository;
import com.condowhats.domain.repository.CondominiumRepository;
import com.condowhats.exception.ConflictException;
import com.condowhats.exception.ResourceNotFoundException;
import com.condowhats.infrastructure.security.JwtProperties;
import com.condowhats.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProps;
    private final AppUserRepository userRepo;
    private final CondominiumRepository condoRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Autentica o usuário e retorna par de tokens.
     * Lança BadCredentialsException se as credenciais estiverem erradas
     * (capturada pelo GlobalExceptionHandler → 401).
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        // Delega a verificação de credenciais ao Spring Security
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        AppUser user = userRepo.findByEmailAndActiveTrue(req.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", req.email()));

        log.info("Login realizado: {} role={}", user.getEmail(), user.getRole());
        return buildTokenPair(user);
    }

    /**
     * Valida o refresh token e emite um novo par de tokens.
     * Lança BadCredentialsException se o token for inválido ou não for do tipo refresh.
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest req) {
        String token = req.refreshToken();

        if (!jwtService.isValid(token)) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Refresh token inválido ou expirado");
        }
        if (!"refresh".equals(jwtService.extractType(token))) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Token fornecido não é um refresh token");
        }

        AppUser user = userRepo.findByEmailAndActiveTrue(jwtService.extractEmail(token))
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                        "Usuário inativo ou não encontrado"));

        return buildTokenPair(user);
    }

    /**
     * Cria um novo usuário do sistema (síndico, manager ou admin).
     * Regras:
     * - E-mail deve ser único
     * - SYNDIC deve ter condominiumId
     * - PLATFORM_ADMIN só pode ser criado por outro PLATFORM_ADMIN (validado no controller via @PreAuthorize)
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        if (userRepo.findByEmailAndActiveTrue(req.email()).isPresent()) {
            throw new ConflictException("E-mail já cadastrado: " + req.email());
        }

        AppUser.UserRole role = resolveRole(req.role());

        if (role == AppUser.UserRole.SYNDIC && req.condominiumId() == null) {
            throw new com.condowhats.exception.BusinessRuleException(
                    "Síndico deve ser vinculado a um condomínio");
        }

        var builder = AppUser.builder()
                .email(req.email())
                .fullName(req.fullName())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(role)
                .active(true);

        if (req.condominiumId() != null) {
            var condo = condoRepo.findById(req.condominiumId())
                    .orElseThrow(() -> new ResourceNotFoundException("Condomínio", req.condominiumId()));
            builder.condominium(condo);
        }

        AppUser saved = userRepo.save(builder.build());
        log.info("Usuário criado: {} role={}", saved.getEmail(), saved.getRole());
        return UserResponse.from(saved);
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private AuthResponse buildTokenPair(AppUser user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                jwtProps.expirationMs(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private AppUser.UserRole resolveRole(String raw) {
        try {
            return AppUser.UserRole.valueOf(raw != null ? raw.toUpperCase() : "SYNDIC");
        } catch (IllegalArgumentException e) {
            throw new com.condowhats.exception.BusinessRuleException("Role inválida: " + raw);
        }
    }
}
