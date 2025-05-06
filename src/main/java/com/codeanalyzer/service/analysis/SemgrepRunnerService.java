package com.codeanalyzer.service.analysis;

import java.util.Map;

public interface SemgrepRunnerService {
    /**
     * Runs Semgrep analysis on the given directory
     * @param directoryPath Path to the directory to analyze
     * @return Map containing Semgrep analysis results including:
     *         - findings: list of security issues found
     *         - severity_counts: count of issues by severity
     *         - total_findings: total number of issues
     *         - exitCode: Semgrep execution status
     */
    Map<String, Object> runAnalysis(String directoryPath);
} 