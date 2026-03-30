package com.blockwin.protocol_api.consensus.mechanism;

import com.blockwin.protocol_api.consensus.UptimeConsensusMechanism;
import com.blockwin.protocol_api.consensus.model.uptime.ReportCategory;
import com.blockwin.protocol_api.consensus.model.uptime.UptimeConsensusResult;
import com.blockwin.protocol_api.health.model.Status;
import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.hub.model.UptimeReport;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UptimeConsensusMechanismTest {

    private UptimeConsensusMechanism mechanism;

    @BeforeEach
    void setup() {
        mechanism = new UptimeConsensusMechanism();
    }

    private UptimeReport report(UUID id, Status status, Continent c, long base) {
        return new UptimeReport(
                id,
                "",
                Instant.now(),
                ReportType.UPTIME,
                c,
                status,
                base, base, base, base, base
        );
    }

    @Test
    void shouldSelectWinnerBasedOnReputationWeight() {

        UUID highRep = UUID.randomUUID();
        UUID lowRep1 = UUID.randomUUID();
        UUID lowRep2 = UUID.randomUUID();

        List<UptimeReport> reports = List.of(
                report(highRep, Status.OK, Continent.EUROPE, 100),
                report(lowRep1, Status.HTTP_5XX, Continent.EUROPE, 90),
                report(lowRep2, Status.HTTP_5XX, Continent.EUROPE, 90)
        );

        Map<UUID, Integer> reputations = Map.of(
                highRep, 10000,
                lowRep1, 100,
                lowRep2, 100
        );

        UptimeConsensusResult result = mechanism.execute(reports, reputations);

        assertEquals(ReportCategory.HEALTHY, result.winnerCategory());

    }

    @Test
    void shouldMarkLatencyOutlierAsFaulty() {

        UUID normal1 = UUID.randomUUID();
        UUID normal2 = UUID.randomUUID();
        UUID outlier = UUID.randomUUID();

        List<UptimeReport> reports = List.of(
                report(normal1, Status.OK, Continent.EUROPE, 10),
                report(normal2, Status.OK, Continent.EUROPE, 12),
                report(outlier, Status.OK, Continent.EUROPE, 1000)
        );

        Map<UUID, Integer> reputations = Map.of(
                normal1, 10000,
                normal2, 10000,
                outlier, 10000
        );

        UptimeConsensusResult result = mechanism.execute(reports, reputations);

        assertFalse(result.validatorCorrectness().get(outlier));
    }

    @Test
    void shouldComputeDifferentRegionsIndependently() {

        UUID eu = UUID.randomUUID();
        UUID asia = UUID.randomUUID();

        List<UptimeReport> reports = List.of(
                report(eu, Status.OK, Continent.EUROPE, 10),
                report(eu, Status.OK, Continent.EUROPE, 20),
                report(eu, Status.OK, Continent.EUROPE, 30),
                report(asia, Status.HTTP_5XX, Continent.ASIA, 20),
                report(asia, Status.HTTP_5XX, Continent.ASIA, 20),
                report(asia, Status.HTTP_5XX, Continent.ASIA, 20)
        );

        Map<UUID, Integer> reputations = Map.of(
                eu, 10000,
                asia, 10000
        );

        UptimeConsensusResult result = mechanism.execute(reports, reputations);

        assertEquals(ReportCategory.HEALTHY, result.categoryByRegion().get(Continent.EUROPE));
        assertEquals(ReportCategory.SERVER_FAILURE, result.categoryByRegion().get(Continent.ASIA));
    }

    @Test
    void shouldHandleSingleValidatorCase() {

        UUID v1 = UUID.randomUUID();

        List<UptimeReport> reports = List.of(
                report(v1, Status.OK, Continent.EUROPE, 10)
        );

        Map<UUID, Integer> reputations = Map.of(v1, 10000);

        UptimeConsensusResult result = mechanism.execute(reports, reputations);

        assertEquals(ReportCategory.UNDEFINED, result.categoryByRegion().get(Continent.EUROPE));
        assertEquals(ReportCategory.UNDEFINED, result.winnerCategory());
    }

    @Test
    void shouldNotAllowValidatorsToControlTheirCorrectnessByChangingRegionMidRound() {

        UUID v = UUID.randomUUID();
        UUID v1 = UUID.randomUUID();
        UUID v2 = UUID.randomUUID();
        UUID v3 = UUID.randomUUID();

        //A malicious validator with id "v" submits a faulty report and changes region mid round by
        // submitting a correct report to overwrite their correctness status in the validatorCorrectness map (false -> true)
        List<UptimeReport> reports = List.of(
                report(v, Status.OK, Continent.EUROPE, 100000),
                report(v1, Status.HTTP_5XX, Continent.EUROPE, 10),
                report(v2, Status.HTTP_5XX, Continent.EUROPE, 10),
                report(v3, Status.HTTP_5XX, Continent.ASIA, 10),
                report(v, Status.HTTP_5XX, Continent.ASIA, 10)
        );

        Map<UUID, Integer> reputations = Map.of(
                v, 10000,
                v1, 10000,
                v2, 10000,
                v3, 10000
        );

        UptimeConsensusResult result = mechanism.execute(reports, reputations);

        assertFalse(result.validatorCorrectness().get(v));
    }
}
