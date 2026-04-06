package com.condowhats.domain.model;

import com.condowhats.domain.port.Channel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "channel_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * NULL quando sharedBot = true.
     * Unicidade controlada pelo service — MySQL trata NULL como distinto em UNIQUE.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominium_id")
    private Condominium condominium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Builder.Default
    @Column(name = "shared_bot", nullable = false)
    private Boolean sharedBot = Boolean.FALSE;

    @Column(name = "credentials_json_enc", nullable = false, columnDefinition = "TEXT")
    private String credentialsJsonEnc;

    @Column(name = "public_identifier", nullable = false, length = 100)
    private String publicIdentifier;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
