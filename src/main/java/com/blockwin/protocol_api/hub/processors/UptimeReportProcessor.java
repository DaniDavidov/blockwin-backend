package com.blockwin.protocol_api.hub.processors;

import com.blockwin.protocol_api.hub.model.Report;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

@Component
public class UptimeReportProcessor implements ReportProcessor {

    @Override
    public Report process(String textMessage) {
        return null;
    }
}
