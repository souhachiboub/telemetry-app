package com.telemetry.backend.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class RootCauseEmbeddable {

    private String sensor;
    private double value;
    private double threshold;
    private String issue;

    public RootCauseEmbeddable() {}

    public RootCauseEmbeddable(String sensor, double value, double threshold, String issue) {
        this.sensor = sensor;
        this.value = value;
        this.threshold = threshold;
        this.issue = issue;
    }

    public String getSensor() { return sensor; }
    public void setSensor(String sensor) { this.sensor = sensor; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }
}
