package com.telemetry.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualTelemetryRequest {

    private String vehicleId;

    private TelemetryDto telemetry;
}