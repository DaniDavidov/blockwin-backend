package com.blockwin.protocol_api.hub.model;

import com.blockwin.protocol_api.hub.ReportType;
import com.blockwin.protocol_api.validator.model.enums.Continent;

import java.util.UUID;

public record MessageEnvelope(UUID validatorId, ReportType reportType, Continent continent, String textMessage) {
}
