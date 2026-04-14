package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.hub.model.RoundState;
import com.blockwin.protocol_api.health.service.UptimeHealthService;
import com.blockwin.protocol_api.platform.model.dto.PlatformDTO;
import com.blockwin.protocol_api.platform.service.PlatformService;
import com.blockwin.protocol_api.reward.model.dto.ActiveEpoch;
import com.blockwin.protocol_api.reward.service.EpochService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Re-populates {@link StateRegistry} on startup with a blank {@link RoundState} for every
 * platform that has an active validation period. Runs after the full application context
 * is started ({@link ApplicationReadyEvent}) so all worker threads are already initialised
 * and blocking on their queues.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StartupRecoveryService {

    private final EpochService epochService;
    private final PlatformService platformService;
    private final UptimeHealthService uptimeHealthService;
    private final StateRegistry stateRegistry;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverActiveRounds() {
        List<ActiveEpoch> activeEpochs = epochService.findAllActivePlatformEpochs();

        if (activeEpochs.isEmpty()) {
            log.info("Startup recovery: no active platforms to restore.");
            return;
        }

        Set<UUID> platformIds = activeEpochs.stream()
                .map(ActiveEpoch::platformId)
                .collect(Collectors.toSet());

        Map<UUID, PlatformDTO> platforms = platformService.findAllByIds(platformIds);

        List<String> urls = platforms.values().stream()
                .map(PlatformDTO::url)
                .toList();
        Map<String, Long> lastRoundIds = uptimeHealthService.findLastCompletedRoundIds(urls);

        for (ActiveEpoch epoch : activeEpochs) {
            PlatformDTO platform = platforms.get(epoch.platformId());
            if (platform == null) {
                log.warn("Startup recovery: skipping orphaned epoch for platformId={}", epoch.platformId());
                continue;
            }

            long interval = platform.checkIntervalSeconds();
            long lastRoundId = (epoch.validationEndTimestamp().getEpochSecond()
                    - epoch.validationStartTimestamp().getEpochSecond()) / interval;

            Long lastCompleted = lastRoundIds.get(platform.url());
            long resumeRoundId = (lastCompleted != null) ? lastCompleted + 1 : 0L;

            RoundState state = new RoundState(
                    epoch.platformId(),
                    platform.url(),
                    interval,
                    epoch.validationStartTimestamp(),
                    lastRoundId,
                    resumeRoundId
            );
            stateRegistry.registerState(state);
            log.info("Recovered platform={} resumeRoundId={} lastRoundId={}",
                    platform.url(), resumeRoundId, lastRoundId);
        }

        log.info("Startup recovery complete - {} active platform(s) restored.", activeEpochs.size());
    }
}
