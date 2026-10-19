package com.kjune922.waterapi.domain;

import lombok.Getter;

@Getter
public enum RiskLevel {

    NORMAL("정상"),
    CAUTION("주의"),
    WARNING("위험");

    private final String description;

    RiskLevel(String description) {
        this.description = description;
    }

}
