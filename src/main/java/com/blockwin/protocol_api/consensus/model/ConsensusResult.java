package com.blockwin.protocol_api.consensus.model;

import com.blockwin.protocol_api.consensus.model.uptime.UptimeConsensusResult;
import com.blockwin.protocol_api.hub.ReportType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;
import java.util.UUID;

/**
 * Type info lets the round.execution Kafka consumer rebuild the concrete result from
 * {@code List<ConsensusResult>}. The discriminator is the existing {@code mechanismType}
 * field, so each new mechanism only needs a {@link JsonSubTypes.Type} entry named after
 * its {@link ReportType}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "mechanismType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = UptimeConsensusResult.class, name = "UPTIME")
})
public interface ConsensusResult {
    ReportType getMechanismType();
    Map<UUID, Boolean> getValidatorCorrectness();
}
