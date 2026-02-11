package com.blockwin.protocol_api.validator.model;

import com.blockwin.protocol_api.validator.model.enums.Continent;
import com.blockwin.protocol_api.validator.model.enums.Country;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "validators")
public class ValidatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    private Continent continent;

    @Enumerated(EnumType.STRING)
    private Country country;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
