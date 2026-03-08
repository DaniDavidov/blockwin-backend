package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.hub.ReportType;

import java.util.UUID;

public record MessageEnvelope(UUID validatorId, ReportType reportType, String textMessage) {
}
