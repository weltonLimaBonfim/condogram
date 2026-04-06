# CondoWhats MVP

SaaS de gestão de condomínios 100% via WhatsApp.
Usa a **Meta Cloud API** (gratuita até 1.000 conversas iniciadas pelo negócio/mês).

## Stack

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.3 |
| Banco | MySQL 8.0 |
| WhatsApp API | Meta Cloud API (sem BSP, sem custo) |
| Event Bus | Spring ApplicationEvents (@Async) |
| Migrations | Flyway |
| Crypto | AES-256-GCM |

---

## Setup em 5 passos

### 1. Conta de desenvolvedor Meta

1. Acesse https://developers.facebook.com e crie um App do tipo **Business**
2. Adicione o produto **WhatsApp**
3. Em *API Setup*, anote:
   - `Phone Number ID`
   - `WhatsApp Business Account ID (WABA ID)`
   - `Temporary Access Token` (gere um permanente em produção)
4. Em *App Settings → Basic*, anote o `App Secret`

### 2. Configurar variáveis de ambiente

```bash
cp .env.example .env
# Edite .env com seus valores reais

# Gere a chave de criptografia:
openssl rand -base64 32
# Cole o resultado em CRYPTO_KEY no .env
```

### 3. Criar templates no Meta Business Manager

Siga o guia completo em `README-TEMPLATES.md`.
São 7 templates UTILITY — aprovação em ~24h.

### 4. Subir o banco

```bash
docker-compose up mysql -d

# Aguardar inicializar (~15s), depois:
# O Flyway roda as migrations automaticamente ao iniciar a aplicação
```

### 5. Rodar a aplicação

```bash
# Carregar variáveis
export $(cat .env | xargs)

# Rodar
./mvnw spring-boot:run

# Expor o webhook (desenvolvimento local)
npx localtunnel --port 8080
# ou: ngrok http 8080

# Configure a URL no painel da Meta:
# https://SEU_TUNEL/webhook
# Verify Token: o mesmo que META_WEBHOOK_VERIFY_TOKEN
```

### 6. Cadastrar o primeiro condomínio

```sql
-- Substitua os valores reais
INSERT INTO condominium (
    management_company_id, name, address, city, state,
    whatsapp_number, wa_phone_number_id, wa_waba_id,
    wa_access_token_enc, status
) VALUES (
    1,
    'Condomínio Exemplo',
    'Rua das Flores, 100',
    'São Paulo', 'SP',
    '+5511999990000',          -- número registrado na Meta (E.164)
    'SEU_PHONE_NUMBER_ID',
    'SEU_WABA_ID',
    'TOKEN_CRIPTOGRAFADO',     -- use TokenCryptoService.encrypt(token)
    'ACTIVE'
);
```

Para criptografar o token, rode o main de teste:
```java
// Temporariamente em CondoWhatsApplication ou um @Component
System.out.println(new TokenCryptoService(cryptoKey).encrypt("seu_access_token"));
```

---

## Arquitetura de eventos

```
Morador → WhatsApp → Meta Cloud API
                          │
                          ▼ POST /webhook
              WhatsAppWebhookController
              (valida HMAC-SHA256)
                          │
                          ▼
              WhatsAppGatewayService
              (resolve condo + morador + sessão)
                          │
              ┌───────────┴──────────────┐
              ▼                          ▼
    EventStoreService            ApplicationEventPublisher
    (interaction_event)                  │
                                         ▼
                             BotOrchestratorService @Async
                             (FSM: 12 estados)
                                         │
                        ┌────────────────┴─────────────────┐
                        ▼                                   ▼
            OccurrenceCreationRequested          ReservationRequested
                        │                                   │
                        ▼                                   ▼
            OccurrenceService @Async            ReservationService @Async
                        │                                   │
                        ▼                                   ▼
                OccurrenceCreated                   ReservationCreated
                        │                                   │
                        └──────────────┬────────────────────┘
                                       ▼
                           NotificationService @Async
                           (envia template via WhatsApp)
```

## Estados da FSM

```
IDLE ──────────────────────────────────────► MAIN_MENU
                                                 │
                    ┌────────────────────────────┤
                    ▼                            ▼
           OCCURRENCE_TITLE              RESERVATION_AREA
                    │                            │
           OCCURRENCE_CATEGORY           RESERVATION_DATE
                    │                            │
           OCCURRENCE_DESC               RESERVATION_TIME
                    │                            │
           OCCURRENCE_CONFIRM ◄──────────RESERVATION_CONFIRM
                    │
                MAIN_MENU
```

## Estrutura de pacotes

```
com.condowhats/
├── CondoWhatsApplication.java
├── config/
│   ├── AsyncConfig.java          — ThreadPool para @Async
│   ├── SecurityConfig.java       — libera /webhook, básico para o resto
│   └── WebClientConfig.java      — WebClient para Meta API
├── domain/
│   ├── event/                    — Spring Application Events (records)
│   ├── model/                    — JPA Entities
│   └── repository/               — JpaRepository interfaces
├── exception/
│   └── GlobalExceptionHandler.java
├── scheduler/
│   └── SessionExpiryScheduler.java  — limpa sessões a cada 5min
├── service/
│   ├── EventStoreService.java       — persiste interaction_event
│   ├── WebhookLogService.java       — log bruto dos webhooks
│   ├── bot/BotOrchestratorService   — FSM central
│   ├── crypto/TokenCryptoService    — AES-256-GCM
│   ├── gateway/WhatsAppGatewayService
│   ├── notification/
│   │   ├── BotMessageSender         — menus interativos (janela 24h)
│   │   └── NotificationService      — templates proativos
│   ├── occurrence/OccurrenceService
│   ├── reservation/ReservationService
│   └── template/
│       ├── TemplateType.java        — enum dos templates aprovados
│       ├── TemplateMessage.java     — DTO + factory methods
│       └── WhatsAppTemplateService  — envia HSM pela Meta API
└── webhook/
    └── WhatsAppWebhookController    — GET verify + POST receive
```
