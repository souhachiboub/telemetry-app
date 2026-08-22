package com.telemetry.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TelemetryFileDto {

    private String vehicleId;

    private List<TelemetryDto> window;
}