package org.example.community.model;

import java.util.List;

public record RiskAnalysisResult(
        String emotion,
        String riskLevel,
        boolean sensitive,
        List<String> matchedSignals
) {
}
