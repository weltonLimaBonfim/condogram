package com.condowhats.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public OpenAPI condoWhatsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CondoWhats API")
                        .description("""
                                API de gestão de condomínios via **Telegram**.
                                
                                **Perfil ativo:** `%s`
                                
                                ## Canal de comunicação
                                Todos os moradores interagem via bot do Telegram (InlineKeyboard com botões).
                                Cada condomínio tem seu próprio bot criado via @BotFather.
                                
                                ## Autenticação JWT
                                1. `POST /api/v1/auth/login` com e-mail e senha
                                2. Copie o `accessToken` retornado
                                3. Clique em **Authorize** e cole: `Bearer <token>`
                                
                                ## Webhook Telegram
                                `POST /telegram/webhook/{botUsername}` — chamado pelo Telegram, autenticado via
                                header `X-Telegram-Bot-Api-Secret-Token`.
                                """.formatted(activeProfile))
                        .version("2.0.0-MVP")
                        .contact(new Contact().name("CondoWhats").email("dev@condowhats.com.br"))
                        .license(new License().name("Proprietário")))
                .servers(serversForProfile())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token obtido em POST /api/v1/auth/login")));
    }

    private List<Server> serversForProfile() {
        return switch (activeProfile) {
            case "prd" -> List.of(new Server().url("https://api.condowhats.com.br").description("Produção"));
            case "hom" -> List.of(
                    new Server().url("https://hom-api.condowhats.com.br").description("Homologação"),
                    new Server().url("http://localhost:8080").description("Local")
            );
            default -> List.of(new Server().url("http://localhost:8080").description("Local (DEV)"));
        };
    }
}
