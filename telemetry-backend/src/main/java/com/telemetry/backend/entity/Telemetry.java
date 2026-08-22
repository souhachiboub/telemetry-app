package com.telemetry.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "telemetry")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Telemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String vehicleId;

    @Column(nullable = false)
    private Instant timestamp;

    private double tripTimeS;
    private double speedKmh;
    private double motorRpm;
    private double motorTempC;

    private double batterySocPct;
    private double batteryVoltageV;
    private double batteryCurrentA;
    private double batteryTempC;

    private double ambientTempC;

    private String brakingActive;
    private String vehicleState;


}