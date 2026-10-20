package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.consensus.model.ConsensusResult;
import com.blockwin.protocol_api.consensus.model.uptime.Latency;
import com.blockwin.protocol_api.consensus.model.uptime.ReportCategory;
import com.blockwin.protocol_api.consensus.model.uptime.UptimeConsensusResult;
import com.blockwin.protocol_api.health.model.Status;
import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Round-trips {@link ExecutedRound} through the same Spring Kafka JSON serializer pair
 * configured for the round.execution topic in application.yml.
 */
class ExecutedRoundSerializationTest {

    private static final String TOPIC = "round.execution";

    @Test
    void executedRound_survivesKafkaJsonRoundTrip_withConcreteConsensusResult() {
        UUID validatorId = UUID.randomUUID();
        UptimeReport report = new UptimeReport("http://localhost:4002", Instant.now(), Status.OK, 10, 20, 30, 40, 100);
        report.setValidatorId(validatorId);
        report.setContinent(Continent.EUROPE);
        report.setReportType(ReportType.UPTIME);

        UptimeConsensusResult result = new UptimeConsensusResult(
                ReportType.UPTIME,
                ReportCategory.HEALTHY,
                10_000,
                Map.of(Continent.EUROPE, ReportCategory.HEALTHY),
                Map.of(Continent.EUROPE, new Latency(10, 20, 30, 40, 100)),
                Map.of(validatorId, true),
                List.of(report)
        );
        Instant start = Instant.parse("2026-10-20T12:00:00Z");
        ExecutedRound round = new ExecutedRound(7, "http://localhost:4002", start, start.plusSeconds(30), List.of(result));

        ExecutedRound decoded = roundTrip(round);

        assertEquals(7, decoded.roundId());
        assertEquals("http://localhost:4002", decoded.platformUrl());
        assertEquals(start, decoded.startTimestamp());
        assertEquals(1, decoded.consensusResults().size());

        ConsensusResult decodedResult = decoded.consensusResults().get(0);
        UptimeConsensusResult uptime = assertInstanceOf(UptimeConsensusResult.class, decodedResult);
        assertEquals(ReportType.UPTIME, uptime.getMechanismType());
        assertEquals(ReportCategory.HEALTHY, uptime.winnerCategory());
        assertEquals(10_000, uptime.certainty());
        assertEquals(ReportCategory.HEALTHY, uptime.categoryByRegion().get(Continent.EUROPE));
        assertEquals(new Latency(10, 20, 30, 40, 100), uptime.latencyByRegion().get(Continent.EUROPE));
        assertEquals(Boolean.TRUE, uptime.getValidatorCorrectness().get(validatorId));
        assertEquals(1, uptime.reports().size());
        assertEquals(Status.OK, uptime.reports().get(0).getStatus());
    }

    private ExecutedRound roundTrip(ExecutedRound round) {
        try (JsonSerializer<ExecutedRound> serializer = new JsonSerializer<>();
             JsonDeserializer<ExecutedRound> deserializer = new JsonDeserializer<>(ExecutedRound.class)) {
            deserializer.addTrustedPackages("*");
            byte[] bytes = serializer.serialize(TOPIC, round);
            return deserializer.deserialize(TOPIC, bytes);
        }
    }
}