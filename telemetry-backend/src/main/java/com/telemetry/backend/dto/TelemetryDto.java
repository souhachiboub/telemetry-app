package com.telemetry.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TelemetryDto {

    @JsonProperty("Trip_Time_s")
    private double tripTimeS;

    @JsonProperty("Speed_kmh")
    private double speedKmh;

    @JsonProperty("Motor_RPM")
    private double motorRpm;

    @JsonProperty("Motor_Temp_C")
    private double motorTempC;

    @JsonProperty("Battery_SOC_pct")
    private double batterySocPct;

    @JsonProperty("Battery_Voltage_V")
    private double batteryVoltageV;

    @JsonProperty("Battery_Current_A")
    private double batteryCurrentA;

    @JsonProperty("Battery_Temp_C")
    private double batteryTempC;

    @JsonProperty("Ambient_Temp_C")
    private double ambientTempC;

    @JsonProperty("Braking_Active")
    private String brakingActive;

    @JsonProperty("Vehicle_State")
    private String vehicleState;


}