package com.blockwin.protocol_api.blockchain.service;

import com.blockwin.protocol_api.blockchain.config.BlockchainConfig;
import com.blockwin.protocol_api.blockchain.model.Transaction;
import com.blockwin.protocol_api.blockchain.model.dto.ChainConfig;
import com.blockwin.protocol_api.blockchain.model.dto.ContractEvent;
import com.blockwin.protocol_api.blockchain.repository.TransactionRepository;
import com.blockwin.protocol_api.validator.model.enums.ChainName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.abi.EventEncoder;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionManagementServiceTest {

    @Mock private MultiChainService multiChainService;
    @Mock private BlockchainConfig blockchainConfig;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionManagementService service;

    private static final String CHAIN_NAME       = "ETHEREUM";
    private static final String TX_HASH          = "0x" + "a".repeat(64);
    private static final String STAKING_CONTRACT = "0x" + "1".repeat(40);
    private static final String VALIDATOR_ADDR   = "0x" + "2".repeat(40);
    private static final String OTHER_ADDR       = "0x" + "3".repeat(40);
    private static final BigInteger STAKE_AMOUNT = BigInteger.valueOf(1_000_000);
    private static final BigInteger TIMESTAMP    = BigInteger.valueOf(1_700_000_000L);

    private ChainConfig chainConfig;

    @BeforeEach
    void setUp() {
        chainConfig = new ChainConfig();
        chainConfig.setStakingContract(STAKING_CONTRACT);
    }


    @Test
    void validateDeposit_transactionAlreadyRecorded_throwsBeforeFetchingReceipt() {
        when(transactionRepository.findByTxHashAndChainName(TX_HASH, ChainName.ETHEREUM))
                .thenReturn(Optional.of(new Transaction()));

        assertThrows(RuntimeException.class,
                () -> service.validateDeposit(TX_HASH, CHAIN_NAME, STAKING_CONTRACT, ContractEvent.DEPOSIT_EVENT));

        verifyNoInteractions(multiChainService);
    }


    @Test
    void validateStakingDeposit_validRequest_savesTransactionWithDecodedFields() {
        when(blockchainConfig.getChain(CHAIN_NAME)).thenReturn(chainConfig);
        when(transactionRepository.findByTxHashAndChainName(TX_HASH, ChainName.ETHEREUM))
                .thenReturn(Optional.empty());
        when(multiChainService.getTransactionReceipt(CHAIN_NAME, TX_HASH))
                .thenReturn(depositReceipt(STAKING_CONTRACT, VALIDATOR_ADDR, STAKE_AMOUNT, TIMESTAMP));

        service.validateStakingDeposit(TX_HASH, CHAIN_NAME, VALIDATOR_ADDR);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals(TX_HASH, saved.getTxHash());
        String rawAddr = VALIDATOR_ADDR.substring(2);
        assertTrue(saved.getValidatorAddress().toLowerCase().contains(rawAddr.toLowerCase()),
                "Saved address should contain the on-chain address hex. Got: " + saved.getValidatorAddress());
        assertEquals(STAKE_AMOUNT, saved.getAmount());
        assertEquals(ChainName.ETHEREUM, saved.getChainName());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void validateStakingDeposit_duplicateTransaction_throws() {
        when(blockchainConfig.getChain(CHAIN_NAME)).thenReturn(chainConfig);
        when(transactionRepository.findByTxHashAndChainName(TX_HASH, ChainName.ETHEREUM))
                .thenReturn(Optional.of(new Transaction()));

        assertThrows(RuntimeException.class,
                () -> service.validateStakingDeposit(TX_HASH, CHAIN_NAME, VALIDATOR_ADDR));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void validateStakingDeposit_addressInEventDoesNotMatchCaller_throwsWithoutSaving() {
        // Receipt emits VALIDATOR_ADDR; caller claims to be OTHER_ADDR
        when(blockchainConfig.getChain(CHAIN_NAME)).thenReturn(chainConfig);
        when(transactionRepository.findByTxHashAndChainName(TX_HASH, ChainName.ETHEREUM))
                .thenReturn(Optional.empty());
        when(multiChainService.getTransactionReceipt(CHAIN_NAME, TX_HASH))
                .thenReturn(depositReceipt(STAKING_CONTRACT, VALIDATOR_ADDR, STAKE_AMOUNT, TIMESTAMP));

        assertThrows(RuntimeException.class,
                () -> service.validateStakingDeposit(TX_HASH, CHAIN_NAME, OTHER_ADDR));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void validateStakingDeposit_receiptHasNoMatchingLog_throwsWithoutSaving() {
        when(blockchainConfig.getChain(CHAIN_NAME)).thenReturn(chainConfig);
        when(transactionRepository.findByTxHashAndChainName(TX_HASH, ChainName.ETHEREUM))
                .thenReturn(Optional.empty());

        // Receipt from a completely different contract — no Deposit log from stakingContract
        TransactionReceipt receipt = depositReceipt("0x" + "9".repeat(40), VALIDATOR_ADDR, STAKE_AMOUNT, TIMESTAMP);
        when(multiChainService.getTransactionReceipt(CHAIN_NAME, TX_HASH)).thenReturn(receipt);

        assertThrows(RuntimeException.class,
                () -> service.validateStakingDeposit(TX_HASH, CHAIN_NAME, VALIDATOR_ADDR));
        verify(transactionRepository, never()).save(any());
    }


    /**
     * Builds a TransactionReceipt containing a single Deposit log ABI-encoded
     * as {@code Deposit(address indexed, uint256 indexed, uint256)}.
     *
     * <p>Topic layout:
     * <pre>
     *   topic[0] = event signature hash
     *   topic[1] = indexed address   — 12 zero bytes + 20-byte address (64 hex chars)
     *   topic[2] = indexed uint256   — 32-byte big-endian amount      (64 hex chars)
     *   data     = non-indexed uint256 — 32-byte big-endian timestamp (64 hex chars)
     * </pre>
     */
    private TransactionReceipt depositReceipt(
            String contractAddress,
            String validatorAddress,
            BigInteger amount,
            BigInteger timestamp
    ) {
        String eventSig      = EventEncoder.encode(ContractEvent.DEPOSIT_EVENT);
        String addrTopic     = "0x000000000000000000000000"
                + validatorAddress.replace("0x", "").replace("0X", "").toLowerCase();
        String amountTopic   = "0x" + String.format("%064x", amount);
        String timestampData = "0x" + String.format("%064x", timestamp);

        Log log = new Log();
        log.setAddress(contractAddress);
        log.setTopics(List.of(eventSig, addrTopic, amountTopic));
        log.setData(timestampData);

        TransactionReceipt receipt = new TransactionReceipt();
        receipt.setLogs(List.of(log));
        return receipt;
    }
}