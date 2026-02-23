package com.blockwin.protocol_api.blockchain.util;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.ArrayList;
import java.util.List;


public class EventDecoder {

    public static String getEventSignature(Event event) {
        return EventEncoder.encode(event);
    }

    public static Log findLogBySignature(
            TransactionReceipt receipt,
            String contractAddress,
            Event event
    ) {
        String eventSignature = getEventSignature(event);
        return receipt.getLogs().stream()
                .filter(log -> contractAddress.equalsIgnoreCase(log.getAddress()))
                .filter(log -> !log.getTopics().isEmpty() && eventSignature.equals(log.getTopics().get(0)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No log found for " + contractAddress));
    }

    public static List<Type> decodeEventLog(Log log, Event event) {
        List<TypeReference<Type>> indexedParams = new ArrayList<>();
        List<TypeReference<Type>> nonIndexedParams = new ArrayList<>();

        for (TypeReference<Type> param : event.getParameters()) {
            if (param.isIndexed()) {
                indexedParams.add(param);
            } else {
                nonIndexedParams.add(param);
            }
        }
        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(
                log.getData(),
                nonIndexedParams
        );
        // (topics[0] is event signature, topics[1+] are indexed params)
        List<Type> indexedValues = new ArrayList<>();
        for (int i = 0; i < indexedParams.size() && i + 1 < log.getTopics().size(); i++) {
            String topic = log.getTopics().get(i + 1);

            // For simple types, decode from topic
            List<Type> decoded = FunctionReturnDecoder.decode(
                    topic,
                    List.of(indexedParams.get(i))
            );

            if (!decoded.isEmpty()) {
                indexedValues.add(decoded.get(0));
            }
        }

        // Merge indexed and non-indexed in original order
        List<Type> result = new ArrayList<>();
        int indexedIdx = 0;
        int nonIndexedIdx = 0;

        for (TypeReference<?> param : event.getParameters()) {
            if (param.isIndexed()) {
                if (indexedIdx < indexedValues.size()) {
                    result.add(indexedValues.get(indexedIdx++));
                }
            } else {
                if (nonIndexedIdx < nonIndexedValues.size()) {
                    result.add(nonIndexedValues.get(nonIndexedIdx++));
                }
            }
        }

        return result;
    }
}
