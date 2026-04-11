package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.platform.event.PlatformUpdateEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class StateUpdateRegistry {
    private final ConcurrentHashMap<String, PlatformUpdateEvent> stateMap = new ConcurrentHashMap<>();

    public void registerUpdate(@NotNull String platformUrl, @NotNull PlatformUpdateEvent event) {
        stateMap.put(platformUrl, event);
    }

    public void removeUpdate(@NotNull String platformUrl) {
        if (!stateMap.containsKey(platformUrl)) {
            return;
        }
        stateMap.remove(platformUrl);
    }

    public PlatformUpdateEvent getUpdate(@NotNull String platformUrl) {
        return stateMap.get(platformUrl);
    }
}
