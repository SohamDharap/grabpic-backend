package com.grabpic.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "td_event_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String venue;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "owner_contact")
    private String ownerContact;

    @Column(name = "photographer_id", nullable = false)
    private Long photographerId;

    @Column(columnDefinition = "uuid")
    private UUID publicToken;

    /**
     * If null, this is a main event.
     * If non-null, this is a sub-event of the referenced parent event.
     * Only one level of nesting is allowed (a sub-event cannot be a parent).
     */
    @Column(name = "parent_event_id")
    private Long parentEventId;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (publicToken == null) {
            publicToken = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
