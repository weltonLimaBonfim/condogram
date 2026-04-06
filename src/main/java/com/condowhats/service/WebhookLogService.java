package com.condowhats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Persiste o payload bruto de cada webhook recebido.
 * Usa JDBC direto (não JPA) para garantir que o log seja salvo
 * mesmo que o processamento principal falhe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookLogService {

    private final JdbcTemplate jdbc;

    @Async
    public void log(String rawBody, Map<String, String> headers,
                    boolean hmacValid, Long condoId, String errorMsg) {
        try {
            jdbc.update(
                    "INSERT INTO webhook_log (condominium_id, raw_body, headers_json, hmac_valid, processed, error_message) VALUES (?,?,?,?,?,?)",
                    condoId,
                    rawBody.length() > 65000 ? rawBody.substring(0, 65000) : rawBody,
                    headersToJson(headers),
                    hmacValid,
                    errorMsg == null,
                    errorMsg
            );
        } catch (Exception e) {
            log.error("Falha ao salvar webhook log", e);
        }
    }

    private String headersToJson(Map<String, String> headers) {
        if (headers == null) return "{}";
        StringBuilder sb = new StringBuilder("{");
        headers.forEach((k, v) -> sb.append("\"").append(k).append("\":\"").append(v).append("\","));
        if (sb.length() > 1) sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}
