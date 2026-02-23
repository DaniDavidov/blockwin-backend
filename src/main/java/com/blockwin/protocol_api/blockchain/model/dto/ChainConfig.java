package com.blockwin.protocol_api.blockchain.model.dto;

import lombok.Data;

@Data
public class ChainConfig {
    private String rpcUrl;
    private Integer chainId;
    private Integer requiredConfirmations;
    private String stakingContract;
}
