package com.blockwin.protocol_api.hub.processors;

import com.blockwin.protocol_api.hub.model.MessageEnvelope;
import com.blockwin.protocol_api.hub.model.Report;
import com.blockwin.protocol_api.hub.model.UptimeReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.blockwin.protocol_api.common.utils.Constants.MAX_LATENCY_MS;

@RequiredArgsConstructor
@Slf4j
@Component
public class UptimeReportProcessor implements ReportProcessor {

    private final ObjectMapper objectMapper;

    @Override
    public Report process(MessageEnvelope messageEnvelope) {
        try {
            String textMessage = messageEnvelope.textMessage();
            UptimeReport uptimeReport = objectMapper.readValue(textMessage, UptimeReport.class);
            if (!isLatencyValid(uptimeReport)) {
                log.warn("Uptime report from validator {} contains out-of-range latency values, message will be dropped",
                        messageEnvelope.validatorId());
                return null;
            }
            uptimeReport.setValidatorId(messageEnvelope.validatorId());
            uptimeReport.setContinent(messageEnvelope.continent());
            uptimeReport.setReportType(messageEnvelope.reportType());
            return uptimeReport;
        } catch (JsonProcessingException e) {
            // Exception is only logged. Otherwise, it would bubble up and kill the worker thread. If the more malicious messages cause this it would lead to DoS.
            log.error("Failed to deserialize uptime report, message will be dropped: {}", e.getMessage());
            return null;
        }
    }

    private boolean isLatencyValid(UptimeReport r) {
        return isInRange(r.getDnsResolutionTime())
                && isInRange(r.getTcpConnectTime())
                && isInRange(r.getTlsHandshakeTime())
                && isInRange(r.getTimeToFirstByte())
                && isInRange(r.getTotalResponseTime());
    }

    private boolean isInRange(long value) {
        return value >= 0 && value <= MAX_LATENCY_MS;
    }
}
