package com.condowhats.domain.port;

import com.condowhats.domain.model.ChannelConfig;
import reactor.core.publisher.Mono;

/**
 * Contrato único para qualquer canal de mensageria.
 * <p>
 * Regras de implementação:
 * - Nunca lançar exceção síncrona — erros retornam Mono.error()
 * - Nunca acessar repositórios — recebe tudo pronto via parâmetros
 * - Responsável apenas por envio; parsing de entrada fica no Gateway do canal
 */
public interface MessagingChannel {

    /**
     * Canal que esta implementação atende
     */
    Channel channel();

    /**
     * Envia uma mensagem para o destinatário.
     *
     * @param config  Configuração do canal para o condomínio (credenciais, etc.)
     * @param message Mensagem normalizada a enviar
     * @return ID externo da mensagem enviada (para rastreamento)
     */
    Mono<String> send(ChannelConfig config, OutboundMessage message);

    /**
     * Envia uma notificação proativa rica (com formatação específica do canal).
     * Implementação padrão chama send() — implementações podem sobrescrever
     * para usar templates aprovados (ex: HSM do WhatsApp).
     */
    default Mono<String> sendNotification(ChannelConfig config, OutboundMessage message) {
        return send(config, message);
    }
}
