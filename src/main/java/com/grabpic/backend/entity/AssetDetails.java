package com.grabpic.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "td_asset_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    /**
     * Optional. When non-null, this asset belongs to the specified sub-event
     * within the parent event identified by eventId.
     */
    @Column(name = "sub_event_id")
    private Long subEventId;

    @Column(name = "photographer_id", nullable = false)
    private Long photographerId;

    @Column(name = "asset_url", nullable = false, length = 1000)
    private String assetUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    private Long size;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * Processing status: UPLOADED, PROCESSING, COMPLETED, FAILED, SKIPPED
     */
    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "UPLOADED";

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
