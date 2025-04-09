package org.example;

import java.time.LocalDateTime;

public class Applicant {
    private String name;
    private String email;
    private LocalDateTime deliveryDateTime;
    private double score;
    private double adjustedScore;

    public Applicant(String name, String email, LocalDateTime deliveryDateTime, double score) {
        this.name = name;
        this.email = email;
        this.deliveryDateTime = deliveryDateTime;
        this.score = score;
        this.adjustedScore = score;
    }

    public String getName() {
        return name;
    }
    public String getLastName(){
        String[] nameParts = name.split("\\s+");
        return nameParts[nameParts.length - 1];
    }
    public String getEmail() {
        return email;
    }
    public LocalDateTime getDeliveryDateTime() {
        return deliveryDateTime;
    }
    public double getScore() {
        return score;
    }
    public double getAdjustedScore() {
        return adjustedScore;
    }

    public void setAdjustedScore(double adjustedScore) {
        this.adjustedScore = adjustedScore;
    }
}
