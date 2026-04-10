package com.blockwin.protocol_api.platform.model;

import com.blockwin.protocol_api.account.model.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "platforms")
public class PlatformEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    private UserEntity owner;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(nullable = false, name = "check_interval_seconds")
    private Long checkIntervalSeconds;

    @Column(nullable = false, name = "validation_end_date")
    private Instant validationEndDate;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
