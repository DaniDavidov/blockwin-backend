package com.blockwin.protocol_api.platform.service;

import com.blockwin.protocol_api.account.model.entity.UserEntity;
import com.blockwin.protocol_api.account.service.UserService;
import com.blockwin.protocol_api.platform.model.PlatformEntity;
import com.blockwin.protocol_api.platform.model.dto.PlatformDTO;
import com.blockwin.protocol_api.platform.model.dto.RegisterPlatformRequest;
import com.blockwin.protocol_api.platform.model.dto.RegisterPlatformResponse;
import com.blockwin.protocol_api.platform.model.error.PlatformNotFoundException;
import com.blockwin.protocol_api.platform.repository.PlatformRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Service
public class PlatformService {
    private final UserService userService;
    private final PlatformRepository platformRepository;

    public RegisterPlatformResponse registerPlatform(RegisterPlatformRequest registerRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails principal = (UserDetails) authentication.getPrincipal();

        UserEntity user = userService.getByUsername(principal.getUsername());
        PlatformEntity platform = PlatformEntity.builder()
                .owner(user)
                .url(registerRequest.url())
                .checkIntervalSeconds(registerRequest.checkIntervalSeconds())
                .createdAt(LocalDateTime.now())
                .build();
        PlatformEntity saved = platformRepository.save(platform);
        return new RegisterPlatformResponse(saved.getId());
    }

    public RegisterPlatformResponse updatePlatform(UUID id, RegisterPlatformRequest registerRequest) {
        PlatformEntity platform = platformRepository.findById(id).orElseThrow(() -> new PlatformNotFoundException(id.toString()));
        platform.setCheckIntervalSeconds(registerRequest.checkIntervalSeconds());
        platform.setUrl(registerRequest.url());
        platform.setUpdatedAt(LocalDateTime.now());
        platformRepository.save(platform);
        return new RegisterPlatformResponse(platform.getId());
    }

    public PlatformDTO getById(UUID id) {
        return platformRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new PlatformNotFoundException(id.toString()));
    }

    private PlatformDTO mapToDTO(PlatformEntity platform) {
        return new PlatformDTO(platform.getUrl(), platform.getCheckIntervalSeconds());
    }
}
