package com.blockwin.protocol_api.hub.processors;

import com.blockwin.protocol_api.hub.model.Report;

public interface ReportProcessor {
    public Report process(String textMessage);
}
