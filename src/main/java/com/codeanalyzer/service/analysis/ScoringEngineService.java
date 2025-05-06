package com.codeanalyzer.service.analysis;

import java.util.Map;

public interface ScoringEngineService {
    /**
     * Calculates the quality score based on analysis results
     * @param analysisResults Results from code analysis
     * @param directoryPath Path to the analyzed directory
     * @return Map containing scoring information including:
     *         - qualityScore: overall quality score (0-100)
     *         - breakdown: detailed scoring breakdown
     *         - recommendations: improvement suggestions
     */
    Map<String, Object> calculateScore(Map<String, Object> analysisResults, String directoryPath);
} 