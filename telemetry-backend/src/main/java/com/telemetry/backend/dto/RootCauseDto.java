package com.telemetry.backend.dto;

public class RootCauseDto {

    private String sensor;
    private double value;
    private double threshold;
    private String issue;

    public RootCauseDto() {}

    public String getSensor() { return sensor; }
    public void setSensor(String sensor) { this.sensor = sensor; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }
}
