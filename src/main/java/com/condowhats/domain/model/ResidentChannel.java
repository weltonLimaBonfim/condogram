package com.condowhats.domain.model;

import com.condowhats.domain.port.Channel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "resident_channel",
        uniqueConstraints = @UniqueConstraint(columnNames = {"resident_id", "channel"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResidentChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Column(name = "external_id", nullable = false, length = 50)
    private String externalId;

    @Column(name = "display_handle", length = 100)
    private String displayHandle;

    @Builder.Default
    @Column(name = "opted_in", nullable = false)
    private Boolean optedIn = Boolean.FALSE;

    @Column(name = "opted_in_at")
    private Instant optedInAt;

    @Column(name = "opted_out_at")
    private Instant optedOutAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
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
