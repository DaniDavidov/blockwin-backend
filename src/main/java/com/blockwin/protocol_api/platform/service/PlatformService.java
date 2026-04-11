package com.blockwin.protocol_api.platform.service;

import com.blockwin.protocol_api.account.model.entity.UserEntity;
import com.blockwin.protocol_api.account.service.UserService;
import com.blockwin.protocol_api.platform.event.CachePlatformEvent;
import com.blockwin.protocol_api.platform.event.PlatformUpdateEvent;
import com.blockwin.protocol_api.platform.model.PlatformEntity;
import com.blockwin.protocol_api.platform.model.dto.PlatformDTO;
import com.blockwin.protocol_api.platform.model.dto.RegisterPlatformRequest;
import com.blockwin.protocol_api.platform.model.dto.RegisterPlatformResponse;
import com.blockwin.protocol_api.platform.model.error.PlatformNotFoundException;
import com.blockwin.protocol_api.platform.repository.PlatformRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class PlatformService {
    private final UserService userService;
    private final PlatformRepository platformRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public RegisterPlatformResponse registerPlatform(RegisterPlatformRequest registerRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails principal = (UserDetails) authentication.getPrincipal();

        UserEntity user = userService.getByUsername(principal.getUsername());
        Instant createdAt = Instant.now();
        Instant validationEndDate = createdAt.plus(registerRequest.validationDays(), ChronoUnit.DAYS);
        PlatformEntity platform = PlatformEntity.builder()
                .owner(user)
                .url(registerRequest.url())
                .checkIntervalSeconds(registerRequest.checkIntervalSeconds())
                .validationEndDate(validationEndDate)
                .createdAt(createdAt)
                .build();
        PlatformEntity saved = platformRepository.save(platform);

        CachePlatformEvent cachePlatformEvent = new CachePlatformEvent(
                this,
                saved.getId(),
                saved.getUrl(),
                saved.getCheckIntervalSeconds(),
                saved.getCreatedAt(),
                saved.getValidationEndDate()
        );
        applicationEventPublisher.publishEvent(cachePlatformEvent);

        return new RegisterPlatformResponse(saved.getId());
    }

    public RegisterPlatformResponse updatePlatform(UUID id, RegisterPlatformRequest registerRequest) {
        PlatformEntity platform = platformRepository.findById(id).orElseThrow(() -> new PlatformNotFoundException(id.toString()));
        platform.setCheckIntervalSeconds(registerRequest.checkIntervalSeconds());
        platform.setUrl(registerRequest.url());
        platform.setUpdatedAt(Instant.now());
        platformRepository.save(platform);

        PlatformUpdateEvent updateEvent = new PlatformUpdateEvent(
                this,
                platform.getId(),
                registerRequest.url(),
                registerRequest.checkIntervalSeconds(),
                platform.getCreatedAt()
        );
        applicationEventPublisher.publishEvent(updateEvent);

        return new RegisterPlatformResponse(platform.getId());
    }

    public PlatformDTO getById(UUID id) {
        return platformRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new PlatformNotFoundException(id.toString()));
    }

    private PlatformDTO mapToDTO(PlatformEntity platform) {
        return new PlatformDTO(platform.getId(), platform.getUrl(), platform.getCheckIntervalSeconds(), platform.getCreatedAt());
    }

    public List<PlatformDTO> getAllPlatforms() {
        return platformRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }
}
