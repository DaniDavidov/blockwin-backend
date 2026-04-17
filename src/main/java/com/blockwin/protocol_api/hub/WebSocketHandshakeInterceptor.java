package com.blockwin.protocol_api.hub;

import com.blockwin.protocol_api.validator.model.ValidatorEntity;
import com.blockwin.protocol_api.validator.model.enums.Continent;
import com.blockwin.protocol_api.validator.model.enums.ValidatorStatus;
import com.blockwin.protocol_api.validator.service.APIKeyService;
import com.blockwin.protocol_api.validator.service.ValidatorService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    private final ValidatorService validatorService;
    private final APIKeyService apiKeyService;
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        String apiKey = servletRequest.getServletRequest()
                .getHeader("X-API-KEY");

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("WebSocket handshake rejected: missing API key from {}", request.getRemoteAddress());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        UUID validatorId = apiKeyService.extractValidatorIdFromAPIKey(apiKey);
        ValidatorEntity validator = validatorService.getValidator(validatorId);
        Continent continent = validator.getContinent();

        if (!validator.getStatus().equals(ValidatorStatus.INACTIVE)) {
            log.warn("WebSocket handshake rejected: validator {} has status {}, expected INACTIVE",
                    validatorId, validator.getStatus());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put("validatorId", validatorId);
        attributes.put("continent", continent);

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
