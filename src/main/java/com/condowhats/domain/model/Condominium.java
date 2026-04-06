package com.condowhats.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "condominium")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Condominium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "management_company_id", nullable = false)
    private ManagementCompany managementCompany;

    @Column(nullable = false)
    private String name;

    @Column
    private String cnpj;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false, columnDefinition = "char(2)", length = 2)
    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "bot_greeting", columnDefinition = "TEXT")
    private String botGreeting;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CondoStatus status = CondoStatus.SETUP;

    @Builder.Default
    @Column(nullable = false)
    private String timezone = "America/Sao_Paulo";

    @OneToMany(mappedBy = "condominium", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChannelConfig> channelConfigs = new ArrayList<>();

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

    public enum CondoStatus {ACTIVE, INACTIVE, SETUP}
}
