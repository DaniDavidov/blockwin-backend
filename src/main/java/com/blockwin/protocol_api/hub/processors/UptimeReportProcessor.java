package com.blockwin.protocol_api.hub.processors;

import com.blockwin.protocol_api.hub.model.Report;
import com.blockwin.protocol_api.hub.model.UptimeReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class UptimeReportProcessor implements ReportProcessor {

    private final ObjectMapper objectMapper;

    @Override
    public Report process(String textMessage) {
        try {
            return objectMapper.readValue(textMessage, UptimeReport.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize uptime report, message will be dropped: {}", e.getMessage());
            return null;
        }
    }
}
