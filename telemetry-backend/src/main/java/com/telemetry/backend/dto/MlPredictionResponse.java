package com.telemetry.backend.dto;

import java.util.List;

public record MlPredictionResponse(
        String status,
        double probability,
        String risk_level,
        int label,
        String model_used,
        List<RootCauseDto> root_causes,
        String recommendation
) {}