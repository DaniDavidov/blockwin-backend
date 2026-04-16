package com.blockwin.protocol_api.blockchain.repository;

import com.blockwin.protocol_api.blockchain.model.Transaction;
import com.blockwin.protocol_api.validator.model.enums.ChainName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    public Optional<Transaction> findByTxHashAndChainName(String txHash, ChainName chainName);
    public Optional<Transaction> findFirstByValidatorAddressOrderByTimestampDesc(String validatorAddress);
}
