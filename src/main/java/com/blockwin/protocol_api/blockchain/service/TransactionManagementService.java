package com.blockwin.protocol_api.blockchain.service;

import com.blockwin.protocol_api.blockchain.config.BlockchainConfig;
import com.blockwin.protocol_api.blockchain.model.Transaction;
import com.blockwin.protocol_api.blockchain.model.dto.ChainConfig;
import com.blockwin.protocol_api.blockchain.model.dto.ContractEvent;
import com.blockwin.protocol_api.blockchain.repository.TransactionRepository;
import com.blockwin.protocol_api.blockchain.util.EventDecoder;
import com.blockwin.protocol_api.validator.model.dto.RegisterValidatorRequest;
import com.blockwin.protocol_api.validator.model.enums.ChainName;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class TransactionManagementService {
    private final MultiChainService multiChainService;
    private final BlockchainConfig blockchainConfig;
    private final SignatureService signatureService;
    private final TransactionRepository transactionRepository;


    @Transactional // prevents race condition caused from the same validator registering twice
    public void validateDeposit(RegisterValidatorRequest request) {
        ChainConfig chainConfig = blockchainConfig.getChain(request.chainName());
        Optional<Transaction> txOpt = transactionRepository.findByTxHashAndChainName(
                request.txHash(),
                ChainName.valueOf(request.chainName())
        );
        if (txOpt.isPresent()) {
            throw new RuntimeException("Transaction already exists");
        }

        TransactionReceipt transactionReceipt = multiChainService.getTransactionReceipt(
                request.chainName(), request.txHash()
        );

        Log log = EventDecoder.findLogBySignature(
                transactionReceipt,
                chainConfig.getStakingContract(),  // Chain-specific address
                ContractEvent.DEPOSIT_EVENT
        );

        List<Type> decodedParams = EventDecoder.decodeEventLog(log, ContractEvent.DEPOSIT_EVENT);

        String validatorAddress = (String) decodedParams.get(0).getValue();
        BigInteger amount = (BigInteger) decodedParams.get(1).getValue();
        BigInteger eventTimestamp = (BigInteger) decodedParams.get(2).getValue();

       if (!validatorAddress.equalsIgnoreCase(request.publicKey())) {
           throw new RuntimeException("Validator address does not match");
       }

        boolean isValid = signatureService.verifySignature(request.message(), request.signature(), validatorAddress);
        if (!isValid) {
            throw new RuntimeException("Signature validation failed");
        }

        Transaction tx = Transaction.builder()
                .txHash(request.txHash())
                .validatorAddress(validatorAddress)
                .amount(amount)
                .chainName(ChainName.valueOf(request.chainName()))
                .timestamp(Instant.now())
                .build();
        transactionRepository.save(tx);
    }
}
