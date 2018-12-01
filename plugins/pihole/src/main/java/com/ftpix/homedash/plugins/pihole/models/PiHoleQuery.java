package com.ftpix.homedash.plugins.pihole.models;

import java.time.LocalDateTime;

public class PiHoleQuery {
    private LocalDateTime date;
    private String type, requestedDomain, requestingClient, answerTypeString;
    private boolean blocked;
    private AnswerType answerType;

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRequestedDomain() {
        return requestedDomain;
    }

    public void setRequestedDomain(String requestedDomain) {
        this.requestedDomain = requestedDomain;
    }

    public String getRequestingClient() {
        return requestingClient;
    }

    public void setRequestingClient(String requestingClient) {
        this.requestingClient = requestingClient;
    }

    public AnswerType getAnswerType() {
        return answerType;
    }

    public void setAnswerType(AnswerType answerType) {
        this.answerType = answerType;
        this.answerTypeString = answerType.getLabel();
        this.blocked = answerType.isBlocked();
    }

    public String getAnswerTypeString() {
        return answerTypeString;
    }

    public boolean isBlocked() {
        return blocked;
    }
}
