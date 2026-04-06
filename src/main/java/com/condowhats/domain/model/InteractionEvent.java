package com.condowhats.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "interaction_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominium_id")
    private Condominium condominium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id")
    private Resident resident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private BotSession session;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction = Direction.INTERNAL;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "json")
    private Map<String, Object> payloadJson = new HashMap<>();

    @Column(name = "external_message_id", length = 100)
    private String externalMessageId;

    @Column(name = "previous_state", length = 60)
    private String previousState;
    @Column(name = "next_state", length = 60)
    private String nextState;
    @Column(name = "processing_ms")
    private Integer processingMs;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @PrePersist
    void onCreate() {
        occurredAt = Instant.now();
    }

    public enum Direction {INBOUND, OUTBOUND, INTERNAL}
}
