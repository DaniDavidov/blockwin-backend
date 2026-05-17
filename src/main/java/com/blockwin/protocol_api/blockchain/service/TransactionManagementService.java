package com.blockwin.protocol_api.blockchain.service;

import com.blockwin.protocol_api.blockchain.config.BlockchainConfig;
import com.blockwin.protocol_api.blockchain.model.Transaction;
import com.blockwin.protocol_api.blockchain.model.dto.ChainConfig;
import com.blockwin.protocol_api.blockchain.model.dto.ContractEvent;
import com.blockwin.protocol_api.blockchain.repository.TransactionRepository;
import com.blockwin.protocol_api.blockchain.util.EventDecoder;
import com.blockwin.protocol_api.reward.model.dto.DepositRewardRequest;
import com.blockwin.protocol_api.validator.model.enums.ChainName;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Service
public class TransactionManagementService {
    private final MultiChainService multiChainService;
    private final BlockchainConfig blockchainConfig;
    private final TransactionRepository transactionRepository;

    /**
     * Validates a staking deposit transaction: verifies the on-chain {@code Deposit} event,
     * checks the validator address, and records the transaction.
     * Identity verification via signed message is handled by the caller before invoking this.
     */
    @Transactional
    public void validateStakingDeposit(String txHash, String chainName, String validatorAddress) {
        ChainConfig chainConfig = blockchainConfig.getChain(chainName);
        List<Type> decodedParams = validateDeposit(
                txHash,
                chainName,
                chainConfig.getStakingContract(),
                ContractEvent.DEPOSIT_EVENT
        );

        String eventAddress = (String) decodedParams.get(0).getValue();
        BigInteger amount = (BigInteger) decodedParams.get(1).getValue();

        if (!eventAddress.equalsIgnoreCase(validatorAddress)) {
            throw new RuntimeException("Validator address does not match deposit event");
        }

        Transaction tx = Transaction.builder()
                .txHash(txHash)
                .validatorAddress(validatorAddress)
                .amount(amount)
                .chainName(ChainName.valueOf(chainName.toUpperCase()))
                .timestamp(Instant.now())
                .build();
        transactionRepository.save(tx);
    }

    @Transactional
    public BigInteger validateRewardDeposit(UUID platformId, DepositRewardRequest request) {
        ChainConfig chainConfig = blockchainConfig.getChain(request.chainName());
        List<Type> decodedParams = validateDeposit(
                request.txHash(),
                request.chainName(),
                chainConfig.getPlatformRegistry(),
                ContractEvent.REWARD_DEPOSITED_EVENT
        );
        String eventOwner = (String) decodedParams.get(0).getValue();
        byte[] eventPlatformId = (byte[]) decodedParams.get(1).getValue();
        BigInteger eventAmount = (BigInteger) decodedParams.get(2).getValue();

        if (!eventOwner.equalsIgnoreCase(request.platformOwnerAddress())) {
            throw new IllegalArgumentException(
                    "Platform owner address mismatch: expected " + request.platformOwnerAddress() + ", got " + eventOwner);
        }
        if (!validatePlatformId(platformId, eventPlatformId)) {
            throw new IllegalArgumentException("Platform ID mismatch in deposit event");
        }
        return eventAmount;
    }

    /**
     * Core deposit validator: checks the transaction has not been processed before, fetches
     * the receipt, finds the matching event log for the given contract, and returns the
     * decoded parameters for the caller to inspect.
     *
     * @param txHash          on-chain transaction hash
     * @param chainName       logical chain name (must match a key in blockchain config)
     * @param contractAddress address of the contract that emitted the event
     * @param event           ABI definition of the expected event
     * @return decoded event parameters in declaration order
     */
    @Transactional
    public List<Type> validateDeposit(String txHash, String chainName, String contractAddress, Event event) {
        Optional<Transaction> txOpt = transactionRepository.findByTxHashAndChainName(
                txHash, ChainName.valueOf(chainName.toUpperCase()));
        if (txOpt.isPresent()) {
            throw new RuntimeException("Transaction already exists");
        }

        TransactionReceipt receipt = multiChainService.getTransactionReceipt(chainName, txHash);
        Log log = EventDecoder.findLogBySignature(receipt, contractAddress, event);
        return EventDecoder.decodeEventLog(log, event);
    }

    private boolean validatePlatformId(UUID platformId, byte[] eventPlatformId) {
        if (eventPlatformId.length != 32) {
            return false;
        }
        byte[] encoded = new byte[32];
        ByteBuffer.wrap(encoded)
                .putLong(platformId.getMostSignificantBits())
                .putLong(platformId.getLeastSignificantBits());

        for (int i = 0; i < eventPlatformId.length; i++) {
            if (encoded[i] != eventPlatformId[i]) {
                return false;
            }
        }
        return true;
    }
}