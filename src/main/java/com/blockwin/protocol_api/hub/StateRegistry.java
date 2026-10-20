package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.hub.model.RoundState;
import com.blockwin.protocol_api.platform.event.CachePlatformEvent;
import com.blockwin.protocol_api.platform.model.dto.PlatformDTO;
import com.blockwin.protocol_api.platform.service.PlatformService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

@Component
public class StateRegistry {
    private final ConcurrentHashMap<String, RoundState> stateMap = new ConcurrentHashMap<>();
    private final DelayQueue<RoundState> expirationQueue = new DelayQueue<>();

    // Every round is queued exactly once, by the single thread that creates it (deposit listener,
    // startup recovery, or the execution worker that took the previous round off the queue).
    public void registerState(@NotNull RoundState state) {
        String platformURL = state.getPlatformURL();
        stateMap.put(platformURL, state);
        expirationQueue.add(state);
    }

    /** Atomically swaps in the next round so reports never see a gap between rounds. */
    public void resetState(@NotNull RoundState newState) {
        stateMap.compute(newState.getPlatformURL(), (key, oldState) -> newState);
        expirationQueue.add(newState);
    }

    /** True if {@code state} is still the live round for its platform (identity, not equality). */
    public boolean isCurrent(@NotNull RoundState state) {
        return stateMap.get(state.getPlatformURL()) == state;
    }

    public RoundState takeExpired() throws InterruptedException {
        return expirationQueue.take();
    }

    public void removeState(@NotNull String platformURL) {
        stateMap.remove(platformURL);
    }

    public RoundState getState(@NotNull String platformURL) {
        return stateMap.get(platformURL);
    }

    public List<PlatformDTO> getActivePlatforms() {
        return stateMap.values().stream()
                .map(s -> new PlatformDTO(
                        s.getPlatformId(),
                        s.getPlatformURL(),
                        s.getCheckIntervalSeconds(),
                        s.getRegistrationTimestamp()
                        ))
                .toList();
    }
}
