package com.blockwin.protocol_api.blockchain.model.dto;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;

import java.util.Arrays;

public class ContractEvent {
    public static final Event DEPOSIT_EVENT = new Event("Deposit", Arrays.asList(
            new TypeReference<Address>(true) {}, // validator address
            new TypeReference<Uint256>(true) {}, // amount in transaction
            new TypeReference<Uint256>() {} // timestamp
    ));
}
