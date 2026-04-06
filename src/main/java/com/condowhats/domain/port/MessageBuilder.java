package com.condowhats.domain.port;

import java.util.List;

/**
 * Factory de OutboundMessages com conteúdo padronizado para cada evento de negócio.
 * Canal-agnóstico — cada canal formata à sua maneira.
 */
public final class MessageBuilder {

    private MessageBuilder() {
    }

    public static OutboundMessage mainMenu(String recipientId, String greeting) {
        String text = greeting + "\n\n*O que deseja fazer?*";
        return OutboundMessage.withButtons(recipientId, text, List.of(
                new OutboundMessage.ButtonRow(List.of(
                        new OutboundMessage.Button("📋 Registrar ocorrência", "MENU_OCCURRENCE"),
                        new OutboundMessage.Button("📅 Reservar área", "MENU_RESERVATION")
                )),
                new OutboundMessage.ButtonRow(List.of(
                        new OutboundMessage.Button("🔍 Minhas ocorrências", "MENU_MY_OCCURRENCES")
                ))
        ));
    }

    public static OutboundMessage categoryPicker(String recipientId) {
        return OutboundMessage.withButtons(recipientId, "Selecione a *categoria* da ocorrência:", List.of(
                new OutboundMessage.ButtonRow(List.of(
                        new OutboundMessage.Button("🔊 Barulho", "NOISE"),
                        new OutboundMessage.Button("🔧 Infraestrutura", "INFRASTRUCTURE")
                )),
                new OutboundMessage.ButtonRow(List.of(
                        new OutboundMessage.Button("🔒 Segurança", "SECURITY"),
                        new OutboundMessage.Button("🧹 Limpeza", "CLEANING")
                )),
                new OutboundMessage.ButtonRow(List.of(
                        new OutboundMessage.Button("🚗 Estacionamento", "PARKING"),
                        new OutboundMessage.Button("❓ Outro", "OTHER")
                ))
        ));
    }

    public static OutboundMessage areaList(String recipientId,
                                           List<OutboundMessage.Button> areaButtons) {
        return OutboundMessage.withButtons(recipientId,
                "*Selecione a área que deseja reservar:*",
                areaButtons.stream()
                        .map(b -> new OutboundMessage.ButtonRow(List.of(b)))
                        .toList()
        );
    }

    public static OutboundMessage confirmationButtons(String recipientId, String summaryText) {
        return OutboundMessage.singleRow(recipientId, summaryText,
                new OutboundMessage.Button("✅ Confirmar", "CONFIRM_YES"),
                new OutboundMessage.Button("❌ Cancelar", "CONFIRM_NO")
        );
    }

    // ── Notificações proativas ────────────────────────────────────────────────

    public static OutboundMessage occurrenceOpened(String recipientId, String ticket, String title) {
        return OutboundMessage.text(recipientId, String.format(
                "✅ *Ocorrência registrada!*\n\n📋 Protocolo: `%s`\n📝 Assunto: %s\n\nRetornaremos em breve.",
                ticket, title
        ));
    }

    public static OutboundMessage occurrenceUpdated(String recipientId, String ticket,
                                                    String status, String note) {
        String emoji = switch (status) {
            case "IN_PROGRESS" -> "🔄";
            case "RESOLVED" -> "✅";
            case "CLOSED" -> "🔒";
            case "CANCELLED" -> "❌";
            default -> "📋";
        };
        return OutboundMessage.text(recipientId, String.format(
                "%s *Atualização da ocorrência*\n\n📋 Protocolo: %s\nStatus: *%s*\n💬 Nota: %s",
                emoji, ticket, status, note
        ));
    }

    public static OutboundMessage reservationConfirmed(String recipientId,
                                                       String area, String date, String time) {
        return OutboundMessage.text(recipientId, String.format(
                "🎉 *Reserva confirmada!*\n\n📍 Área: *%s*\n📅 Data: *%s*\n🕐 Horário: *%s*",
                area, date, time
        ));
    }

    public static OutboundMessage announcement(String recipientId, String subject, String body) {
        return OutboundMessage.text(recipientId, String.format(
                "📢 *%s*\n\n%s\n\n_Administração do condomínio_",
                subject, body
        ));
    }
}
