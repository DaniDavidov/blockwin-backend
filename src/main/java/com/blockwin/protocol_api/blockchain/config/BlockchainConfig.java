package com.blockwin.protocol_api.blockchain.config;

import com.blockwin.protocol_api.blockchain.model.dto.ChainConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "blockchain")
@Configuration
public class BlockchainConfig {

    private Map<String, ChainConfig> chains = new HashMap<>();

    public ChainConfig getChain(String chainName) {
        ChainConfig config = chains.get(chainName);
        if (config == null) {
            throw new IllegalArgumentException("Chain not configured: " + chainName);
        }
        return config;
    }
}
