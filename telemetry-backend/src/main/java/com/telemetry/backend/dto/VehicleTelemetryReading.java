package com.telemetry.backend.dto;

public record VehicleTelemetryReading(
        Double Trip_Time_s,
        Double Speed_kmh,
        Double Motor_RPM,
        Double Motor_Temp_C,
        Double Battery_SOC_pct,
        Double Battery_Voltage_V,
        Double Battery_Current_A,
        Double Battery_Temp_C,
        Double Ambient_Temp_C,
        String Braking_Active,
        String Vehicle_State
) {}