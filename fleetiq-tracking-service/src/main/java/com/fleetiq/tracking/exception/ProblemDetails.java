package com.fleetiq.tracking.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetails(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    OffsetDateTime timestamp,
    
    @JsonProperty("correlation_id")
    String correlationId,
    
    @JsonProperty("invalid_params")
    List<InvalidParam> invalidParams
) {
    public record InvalidParam(String name, String reason) {}
}
