package com.codeanalyzer.service;

import java.util.Map;

public interface CodeAnalysisService {
    /**
     * Analyzes the code in the given directory and returns a quality score
     * @param directoryPath Path to the directory containing code to analyze
     * @return Map containing analysis results and quality score
     */
    Map<String, Object> analyzeCode(String directoryPath);
} 