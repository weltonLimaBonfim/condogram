package com.condowhats.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "resident")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * NULL para placeholder antes da identificação por CPF
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominium_id")
    private Condominium condominium;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 11, columnDefinition = "char(11)")
    private String cpf;

    @Column(name = "unit_number", nullable = false, length = 20)
    private String unitNumber;

    @Column
    private String block;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResidentRole role = ResidentRole.RESIDENT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResidentStatus status = ResidentStatus.ACTIVE;

    @OneToMany(mappedBy = "resident", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResidentChannel> channels;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static String normalizeCpf(String raw) {
        return raw == null ? null : raw.replaceAll("[^0-9]", "");
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum ResidentRole {RESIDENT, OWNER, SYNDIC, ADMIN}

    public enum ResidentStatus {ACTIVE, INACTIVE, BLOCKED}
}
