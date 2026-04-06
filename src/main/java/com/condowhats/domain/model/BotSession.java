package com.condowhats.domain.model;

import com.condowhats.domain.port.Channel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "bot_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    /**
     * NULL enquanto o morador ainda não se identificou via CPF
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominium_id")
    private Condominium condominium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Builder.Default
    @Column(nullable = false, length = 60)
    private String state = BotState.IDENTIFYING.name();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", columnDefinition = "json")
    private Map<String, Object> contextData = new HashMap<>();

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(nullable = false)
    private Instant lastActivityAt;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = lastActivityAt = Instant.now();
        expiresAt = Instant.now().plusSeconds(1800);
    }

    public void touch() {
        lastActivityAt = Instant.now();
        expiresAt = Instant.now().plusSeconds(1800);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public BotState getBotState() {
        return BotState.valueOf(state);
    }

    public void setBotState(BotState s) {
        this.state = s.name();
    }

    public boolean isIdentified() {
        return condominium != null;
    }
}
