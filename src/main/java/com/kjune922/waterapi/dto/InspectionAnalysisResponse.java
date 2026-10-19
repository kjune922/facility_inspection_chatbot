package com.kjune922.waterapi.dto;

import com.kjune922.waterapi.domain.RiskLevel;

@.Getter
@.Setter
public class InspectionAnalysisResponse {

    private String summary;
    private String abnormalityType;
    private RiskLevel riskLevel;
    private String recommendedAction;

    public InspectionAnalysisResponse() {
    }

    public InspectionAnalysisResponse(String summary, String abnormalityType, RiskLevel riskLevel, String recommendedAction) {
        this.summary = summary;
        this.abnormalityType = abnormalityType;
        this.riskLevel = riskLevel;
        this.recommendedAction = recommendedAction;
    }

}
