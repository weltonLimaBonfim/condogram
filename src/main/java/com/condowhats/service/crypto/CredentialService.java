package com.condowhats.service.crypto;

import com.condowhats.domain.model.ChannelConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Descriptografa e parseia as credenciais de um ChannelConfig.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialService {

    private final TokenCryptoService crypto;
    private final ObjectMapper objectMapper;

    public Map<String, String> decrypt(ChannelConfig config) {
        try {
            String json = crypto.decrypt(config.getCredentialsJsonEnc());
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Falha ao descriptografar credenciais do canal " + config.getChannel(), e);
        }
    }

    public String encrypt(Map<String, String> credentials) {
        try {
            return crypto.encrypt(objectMapper.writeValueAsString(credentials));
        } catch (Exception e) {
            throw new RuntimeException("Falha ao criptografar credenciais", e);
        }
    }

    public String get(ChannelConfig config, String key) {
        Map<String, String> creds = decrypt(config);
        String value = creds.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Credencial ausente: " + key + " no canal " + config.getChannel());
        }
        return value;
    }
}
