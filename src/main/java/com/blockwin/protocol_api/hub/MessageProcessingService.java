package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.hub.model.MessageEnvelope;
import com.blockwin.protocol_api.hub.model.Report;
import com.blockwin.protocol_api.hub.model.ReportBitmap;
import com.blockwin.protocol_api.hub.model.RoundState;
import com.blockwin.protocol_api.hub.processors.ReportProcessor;
import com.blockwin.protocol_api.hub.processors.UptimeReportProcessor;
import com.blockwin.protocol_api.platform.event.CachePlatformEvent;
import com.blockwin.protocol_api.platform.event.PlatformUpdateEvent;
import com.blockwin.protocol_api.platform.service.PlatformService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;


@Slf4j
@Service
public class MessageProcessingService {
    private final IngestionService ingestionService;
    private final StateRegistry stateRegistry;
    private final StateUpdateRegistry stateUpdateRegistry;
    private final HashMap<ReportType, ReportProcessor> reportProcessors;
    private static final int NUMBER_OF_WORKERS = 3;

    public MessageProcessingService(
            IngestionService ingestionService,
            UptimeReportProcessor uptimeReportProcessor,
            StateRegistry stateRegistry,
            StateUpdateRegistry stateUpdateRegistry
    ) {
        this.ingestionService = ingestionService;
        this.stateRegistry = stateRegistry;
        this.stateUpdateRegistry = stateUpdateRegistry;
        this.reportProcessors = new HashMap<>();
        this.reportProcessors.put(ReportType.UPTIME, uptimeReportProcessor);
    }

    @PostConstruct
    public void initializeWorkers() {
        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
            startWorker();
        }
    }

    private Report processMessage(MessageEnvelope messageEnvelope) throws InterruptedException {
        ReportType reportType = messageEnvelope.reportType();
        ReportProcessor reportProcessor = reportProcessors.get(reportType);
        if (reportProcessor == null) {
            throw new RuntimeException("Unknown message type: " + reportType);
        }
        return reportProcessor.process(messageEnvelope.textMessage());
    }

    private void persistReport(Report incomingReport) {
        RoundState state = stateRegistry.getState(incomingReport.getPlatformUrl());
        if (state == null) {
            throw new RuntimeException("Unknown platform URL: " + incomingReport.getPlatformUrl());
        }

        ReportBitmap bitmap = state
                .getBitmapByValidator()
                .computeIfAbsent(incomingReport.getValidatorId(), uuid -> new ReportBitmap());
        boolean success = bitmap.markSubmitted(incomingReport.getReportType());
        if (!success) {
            throw new RuntimeException("The validator has already submitted a report for this round.");
        }

        if (state.getRoundId() == -1) {
            initializeState(state);
        }

        state.getReportsByType().computeIfPresent(
                incomingReport.getReportType(),
                (reportType, reports) -> {
                    reports.add(incomingReport);
                    return reports;
                }
        );
    }

    private void initializeState(RoundState state) {
        long span = Instant.now().getEpochSecond() - state.getRegistrationTimestamp().getEpochSecond();
        state.setRoundId(span / state.getCheckIntervalSeconds());
        state.setFinalized(false);
        state.setExpiration(Instant.now().plusSeconds(state.getCheckIntervalSeconds()));
        state.setStartTimestamp(Instant.now());
        stateRegistry.launchState(state.getPlatformURL());
    }

    private void startWorker() {
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    MessageEnvelope messageEnvelope = this.ingestionService.dequeueMessage();
                    Report report = processMessage(messageEnvelope);
                    if (report == null) {
                        continue;
                    }
                    persistReport(report);
                } catch (InterruptedException e) {
                    log.atError().log(e.getMessage());
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    @Order(1)
    @EventListener
    public void cachePlatform(CachePlatformEvent cachePlatformEvent) {
        if (!(cachePlatformEvent.getSource() instanceof PlatformService)) {
            return;
        }
        RoundState state = new RoundState(
                cachePlatformEvent.getPlatformURL(),
                cachePlatformEvent.getCheckIntervalSeconds(),
                cachePlatformEvent.getRegistrationTimestamp()
        );
        stateRegistry.registerState(state);
    }

    @Order(1)
    @EventListener
    public void cacheUpdate(PlatformUpdateEvent event) {
        if (!(event.getSource() instanceof PlatformService)) {
            return;
        }
        stateUpdateRegistry.registerUpdate(event.getPlatformURL(), event);
    }
}
