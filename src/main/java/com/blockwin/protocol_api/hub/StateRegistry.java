package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.hub.model.RoundState;
import com.blockwin.protocol_api.platform.event.CachePlatformEvent;
import com.blockwin.protocol_api.platform.service.PlatformService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

@Component
public class StateRegistry {
    private final ConcurrentHashMap<String, RoundState> stateMap = new ConcurrentHashMap<>();
    private final DelayQueue<RoundState> expirationQueue = new DelayQueue<>();

    public void registerState(@NotNull RoundState state) {
        String platformURL = state.getPlatformURL();
        stateMap.put(platformURL, state);
        expirationQueue.add(state);
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
}
