package com.gft.similarproducts.infrastructure.adapter.in.rest;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String message) {
}
