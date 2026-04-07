package com.condowhats.adapter.in.messaging.telegram;

import com.condowhats.domain.model.ChannelConfig;
import com.condowhats.domain.port.Channel;
import com.condowhats.domain.repository.ChannelConfigRepository;
import com.condowhats.service.crypto.CredentialService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Long-polling da Bot API do Telegram.
 * <p>
 * Ativo apenas quando condowhats.telegram.mode=POLLING (case-insensitive via ignoreCase).
 * <p>
 * Motivos comuns de polling não funcionar:
 * 1. Webhook registrado na Bot API — o Telegram NÃO entrega via getUpdates se houver webhook ativo.
 * Solução: chamar deleteWebhook antes de iniciar o polling (feito automaticamente aqui).
 * 2. URL com colchetes [] não codificados — a Telegram Bot API recusa allowed_updates mal-formado.
 * Solução: usar UriComponentsBuilder para codificação correta.
 * 3. @ConditionalOnProperty case-sensitive — "Polling" não ativa o bean, só "POLLING".
 * Solução: ignoreCase = true.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "condowhats.telegram.mode",
        havingValue = "POLLING",
        matchIfMissing = false
)
public class TelegramPollingService {

    private static final int POLL_TIMEOUT_SEC = 30;
    private static final int READ_TIMEOUT_SEC = 45;
    private static final long RETRY_DELAY_MS = 5_000;
    private static final long STARTUP_DELAY_MS = 3_000;

    private final TelegramGatewayService gatewayService;
    private final ChannelConfigRepository channelConfigRepo;
    private final CredentialService credentialService;
    private final TelegramProperties properties;
    private final ObjectMapper objectMapper;

    private WebClient pollingClient;
    private volatile boolean running = false;
    private ExecutorService executor;

    private final ConcurrentHashMap<String, AtomicLong> offsets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Future<?>> activePollers = new ConcurrentHashMap<>();

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @PostConstruct
    public void start() {
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║   Telegram POLLING iniciado  (timeout={}s)          ║", POLL_TIMEOUT_SEC);
        log.info("╚══════════════════════════════════════════════════════╝");

        // WebClient com timeout estendido para suportar long-poll de 30s
        HttpClient netty = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SEC)));

        pollingClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(netty))
                .build();

        running = true;
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("tg-poll-" + t.getId());
            return t;
        });

        executor.submit(() -> {
            sleep(STARTUP_DELAY_MS);
            syncPollers();
        });
    }

    @PreDestroy
    public void stop() {
        log.info("Encerrando Telegram POLLING...");
        running = false;
        executor.shutdownNow();
    }

    // ── Sincronização periódica ───────────────────────────────────────────────

    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    public void syncPollers() {
        if (!running) return;

        List<ChannelConfig> configs;
        try {
            configs = channelConfigRepo.findAllByChannelAndActiveTrue(Channel.TELEGRAM);
        } catch (Exception e) {
            log.warn("syncPollers: erro ao buscar configs — {}", e.getMessage());
            return;
        }

        if (configs.isEmpty()) {
            log.info("Polling: nenhum ChannelConfig TELEGRAM ativo no banco.");
            return;
        }

        for (ChannelConfig cfg : configs) {
            String identifier = cfg.getPublicIdentifier();
            Future<?> existing = activePollers.get(identifier);
            if (existing != null && !existing.isDone()) {
                continue;
            }
            offsets.putIfAbsent(identifier, new AtomicLong(0));
            Future<?> f = executor.submit(() -> pollLoop(cfg));
            activePollers.put(identifier, f);
            log.info("Polling (re)iniciado para @{}", identifier);
        }
    }

    // ── Loop de polling por bot ───────────────────────────────────────────────

    private void pollLoop(ChannelConfig config) {
        String identifier = config.getPublicIdentifier();

        // Antes de iniciar o long-poll, garante que não há webhook registrado.
        // O Telegram NÃO entrega mensagens via getUpdates enquanto houver webhook ativo.
        deleteWebhookIfPresent(config, identifier);

        log.info("Thread de polling ativa: @{}", identifier);

        while (running) {
            try {
                poll(config);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Erro no polling @{}: {} — retry em {}ms",
                        identifier, e.getMessage(), RETRY_DELAY_MS);
                sleep(RETRY_DELAY_MS);
            }
        }

        log.info("Thread encerrada: @{}", identifier);
    }

    // ── Remoção de webhook ────────────────────────────────────────────────────

    /**
     * Chama deleteWebhook na Bot API.
     * Sem isso, se houver webhook registrado, getUpdates retorna sempre vazio —
     * o bot parece "não receber nada" mesmo com polling rodando.
     */
    private void deleteWebhookIfPresent(ChannelConfig config, String identifier) {
        try {
            String token = credentialService.get(config, "botToken");

            // Primeiro verifica se há webhook registrado
            String infoUrl = properties.apiBaseUrl() + "/bot" + token + "/getWebhookInfo";
            String infoBody = pollingClient.get()
                    .uri(infoUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (infoBody == null) return;
            JsonNode info = objectMapper.readTree(infoBody);
            String webhookUrl = info.at("/result/url").asText("");

            if (webhookUrl.isBlank()) {
                log.info("@{} — sem webhook registrado, polling pode iniciar.", identifier);
                return;
            }

            log.warn("@{} — webhook ativo detectado: {}. Removendo...", identifier, webhookUrl);

            // Remove o webhook
            String deleteUrl = properties.apiBaseUrl() + "/bot" + token + "/deleteWebhook";
            String deleteBody = pollingClient.post()
                    .uri(deleteUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode deleteResp = objectMapper.readTree(deleteBody);
            if (deleteResp.path("ok").asBoolean(false)) {
                log.info("@{} — webhook removido com sucesso. Polling iniciará agora.", identifier);
            } else {
                log.error("@{} — falha ao remover webhook: {}", identifier, deleteBody);
            }

        } catch (Exception e) {
            log.warn("@{} — erro ao verificar/remover webhook: {}", identifier, e.getMessage());
        }
    }

    // ── Poll individual ───────────────────────────────────────────────────────

    private void poll(ChannelConfig config) throws Exception {
        String identifier = config.getPublicIdentifier();
        long offset = offsets.get(identifier).get();
        String token;

        try {
            token = credentialService.get(config, "botToken");
        } catch (Exception e) {
            log.error("botToken ausente/inválido para @{}: {}", identifier, e.getMessage());
            sleep(RETRY_DELAY_MS);
            return;
        }

        // Monta URI com UriComponentsBuilder — garante codificação correta dos []
        // Sem isso, allowed_updates com colchetes literais pode ser rejeitado pela API
        URI uri = UriComponentsBuilder
                .fromHttpUrl(properties.apiBaseUrl() + "/bot" + token + "/getUpdates")
                .queryParam("timeout", POLL_TIMEOUT_SEC)
                .queryParam("offset", offset)
                .queryParam("allowed_updates", "message,callback_query")
                .build()
                .toUri();

        log.debug("getUpdates @{} offset={}", identifier, offset);

        String body = pollingClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(READ_TIMEOUT_SEC))
                .block();

        if (body == null || body.isBlank()) return;

        JsonNode resp = objectMapper.readTree(body);

        if (!resp.path("ok").asBoolean(false)) {
            int code = resp.path("error_code").asInt();
            String desc = resp.path("description").asText("?");
            log.warn("getUpdates ok=false [@{}] code={} — {}", identifier, code, desc);

            if (code == 401) {
                log.error("Token inválido para @{} — encerrando thread.", identifier);
                return;
            }
            // 409 Conflict = outro processo também está fazendo polling — não deve acontecer
            if (code == 409) {
                log.error("Conflito de polling para @{} — outra instância ativa?", identifier);
                sleep(10_000);
                return;
            }
            sleep(RETRY_DELAY_MS);
            return;
        }

        JsonNode updates = resp.path("result");
        if (!updates.isArray() || updates.isEmpty()) {
            log.debug("Nenhum update novo para @{}", identifier);
            return;
        }

        log.info("{} update(s) recebido(s) de @{}", updates.size(), identifier);

        for (JsonNode update : updates) {
            long updateId = update.path("update_id").asLong();
            try {
                gatewayService.processUpdate(update, identifier);
            } catch (Exception e) {
                log.error("Erro ao processar update_id={} @{}: {}",
                        updateId, identifier, e.getMessage(), e);
            }
            // Avança offset mesmo em erro de processamento — evita loop infinito
            offsets.get(identifier).set(updateId + 1);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
