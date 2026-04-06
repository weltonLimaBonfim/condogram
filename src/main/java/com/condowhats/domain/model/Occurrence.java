package com.condowhats.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "occurrence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Occurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_event_id")
    private InteractionEvent originEvent;

    @Column(name = "ticket_number", nullable = false, length = 20)
    private String ticketNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccurrenceCategory category = OccurrenceCategory.OTHER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccurrenceStatus status = OccurrenceStatus.OPEN;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

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

    public enum OccurrenceCategory {NOISE, INFRASTRUCTURE, SECURITY, CLEANING, ANIMAL, PARKING, OTHER}

    public enum OccurrenceStatus {OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED}

    public enum Priority {LOW, MEDIUM, HIGH, URGENT}
}
