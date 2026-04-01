package com.blockwin.protocol_api.health.model;

public enum Status {
    OK,
    DNS_FAILURE,
	TCP_TIMEOUT,
	TLS_FAILURE,
	CONNECTION_REFUSED,
	HTTP_5XX,
	HTTP_4XX,
	TIMEOUT,
	INVALID_CERT
}
