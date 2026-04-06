package com.condowhats.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "common_area")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;

    @Column(nullable = false)
    private String name;
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Integer capacity = 0;

    @Builder.Default
    @Column(name = "advance_days_limit", nullable = false)
    private Integer advanceDaysLimit = 30;

    @Builder.Default
    @Column(name = "max_duration_hours", nullable = false)
    private Integer maxDurationHours = 4;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "available_days", nullable = false, columnDefinition = "json")
    private List<String> availableDays;

    @Column(name = "available_from", nullable = false)
    private LocalTime availableFrom;
    @Column(name = "available_until", nullable = false)
    private LocalTime availableUntil;

    @Builder.Default
    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = Boolean.FALSE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
