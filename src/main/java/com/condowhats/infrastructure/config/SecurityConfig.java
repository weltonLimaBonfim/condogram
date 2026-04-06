package com.condowhats.infrastructure.config;

import com.condowhats.domain.repository.AppUserRepository;
import com.condowhats.infrastructure.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/telegram/webhook/**",     // Telegram Bot API
            "/whatsapp/webhook",        // WhatsApp Meta API (GET verify + POST receive)
            "/api/v1/auth/**",
            "/actuator/health",
            "/actuator/health/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs",
            "/api-docs/**",
            "/api-docs.yaml"
    };
    private final JwtAuthFilter jwtAuthFilter;
    private final AppUserRepository userRepo;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/v1/condominiums/**").hasAnyRole("PLATFORM_ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/condominiums/*/residents/**").hasAnyRole("PLATFORM_ADMIN", "MANAGER", "SYNDIC")
                        .requestMatchers("/api/v1/condominiums/*/occurrences/**").hasAnyRole("PLATFORM_ADMIN", "MANAGER", "SYNDIC")
                        .requestMatchers("/api/v1/condominiums/*/reservations/**").hasAnyRole("PLATFORM_ADMIN", "MANAGER", "SYNDIC")
                        .requestMatchers("/api/v1/condominiums/*/areas/**").hasAnyRole("PLATFORM_ADMIN", "MANAGER", "SYNDIC")
                        .requestMatchers("/api/v1/condominiums/*/notifications/**").hasAnyRole("PLATFORM_ADMIN", "MANAGER", "SYNDIC")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> userRepo.findByEmailAndActiveTrue(email)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .roles(user.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        var p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService());
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
