package com.blockwin.protocol_api.blockchain.service;

import com.blockwin.protocol_api.blockchain.config.BlockchainConfig;
import com.blockwin.protocol_api.blockchain.model.dto.ChainConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@AllArgsConstructor
@Service
public class MultiChainService {
    private final BlockchainConfig blockchainConfig;
    private final Map<String, Web3j> web3jClients = new ConcurrentHashMap<>();

    public Web3j getWeb3j(String chainName) {
        Web3j web3j = web3jClients.get(chainName);
        if (web3j == null) {
            ChainConfig chainConfig = blockchainConfig.getChain(chainName);
            web3j = Web3j.build(new HttpService(chainConfig.getRpcUrl()));
            web3jClients.put(chainName, web3j);
        }
        return web3j;
    }

    public TransactionReceipt getTransactionReceipt(String chainName, String transactionHash) {
        try {
            Web3j web3j = getWeb3j(chainName);
            return web3j.ethGetTransactionReceipt(transactionHash)
                    .send()
                    .getTransactionReceipt().orElseThrow(() -> new RuntimeException("Transaction not found"));
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Transaction not found");
        }
    }
}
