package com.condowhats.domain.port;

/**
 * Canais de mensageria suportados.
 * Adicionar um novo canal = criar um novo enum value + implementar MessagingChannel.
 */
public enum Channel {
    TELEGRAM,
    WHATSAPP
}
