package com.codeanalyzer.service.analysis.impl;

import com.codeanalyzer.service.analysis.ScoringEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class ScoringEngineServiceImpl implements ScoringEngineService {

    // Scoring constants
    private static final double BASE_SCORE = 100.0;
    private static final double ERROR_LOG_PENALTY = 20.0;
    private static final double WARNING_PENALTY = 0.5;
    private static final double INFO_PENALTY = 1.0 / 50.0;
    private static final int LINES_PER_THOUSAND = 1000;
    
    // Bonus thresholds
    private static final double TEST_COVERAGE_BONUS = 5.0;
    private static final double CLEAN_CODE_BONUS = 3.0;
    private static final double DOCUMENTATION_BONUS = 2.0;

    @Override
    public Map<String, Object> calculateScore(Map<String, Object> analysisResults, String directoryPath) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (analysisResults == null || !analysisResults.containsKey("findings")) {
                result.put("error", "Invalid analysis results");
                return result;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> findings = (List<Map<String, Object>>) analysisResults.get("findings");
            if (findings.isEmpty()) {
                result.put("qualityScore", BASE_SCORE);
                result.put("breakdown", createEmptyBreakdown());
                return result;
            }

            // Count findings by severity
            int errorCount = 0;
            int warningCount = 0;
            int infoCount = 0;

            for (Map<String, Object> finding : findings) {
                String severity = ((String) finding.get("severity")).toLowerCase();
                switch (severity) {
                    case "error": errorCount++; break;
                    case "warning": warningCount++; break;
                    case "info": infoCount++; break;
                }
            }

            // Calculate lines of code
            long totalLines = countLinesOfCode(directoryPath);
            double normalizedFactor = Math.max(1.0, totalLines / (double)LINES_PER_THOUSAND);

            // Calculate base score with logarithmic penalty for errors
            double score = BASE_SCORE;
            score -= (Math.log(1 + errorCount) / Math.log(2)) * ERROR_LOG_PENALTY;
            score -= (warningCount * WARNING_PENALTY) / normalizedFactor;
            score -= (infoCount * INFO_PENALTY) / normalizedFactor;

            // Add bonuses
            double bonus = calculateBonus(analysisResults, directoryPath);
            score += bonus;

            // Ensure score is within bounds
            score = Math.max(0.0, Math.min(100.0, score));

            // Prepare detailed breakdown
            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("base_score", BASE_SCORE);
            breakdown.put("error_penalty", (Math.log(1 + errorCount) / Math.log(2)) * ERROR_LOG_PENALTY);
            breakdown.put("warning_penalty", (warningCount * WARNING_PENALTY) / normalizedFactor);
            breakdown.put("info_penalty", (infoCount * INFO_PENALTY) / normalizedFactor);
            breakdown.put("bonus", bonus);
            breakdown.put("normalized_factor", normalizedFactor);
            breakdown.put("total_lines", totalLines);

            // Prepare recommendations
            List<String> recommendations = generateRecommendations(findings);

            result.put("qualityScore", score);
            result.put("breakdown", breakdown);
            result.put("recommendations", recommendations);

        } catch (Exception e) {
            log.error("Error calculating score: {}", e.getMessage());
            result.put("error", "Failed to calculate score: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> createEmptyBreakdown() {
        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("base_score", BASE_SCORE);
        breakdown.put("error_penalty", 0.0);
        breakdown.put("warning_penalty", 0.0);
        breakdown.put("info_penalty", 0.0);
        breakdown.put("bonus", 0.0);
        breakdown.put("normalized_factor", 1.0);
        breakdown.put("total_lines", 0);
        return breakdown;
    }

    private long countLinesOfCode(String directoryPath) throws IOException {
        return Files.walk(Paths.get(directoryPath))
                .filter(path -> path.toString().endsWith(".java"))
                .mapToLong(this::countLinesInFile)
                .sum();
    }

    private long countLinesInFile(Path path) {
        try {
            return Files.lines(path).count();
        } catch (IOException e) {
            log.error("Error counting lines in file {}: {}", path, e.getMessage());
            return 0;
        }
    }

    private double calculateBonus(Map<String, Object> analysisResults, String directoryPath) {
        double bonus = 0.0;

        try {
            // Test coverage bonus
            if (hasGoodTestCoverage(directoryPath)) {
                bonus += TEST_COVERAGE_BONUS;
            }

            // Clean code bonus (fewer warnings relative to code size)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> findings = (List<Map<String, Object>>) analysisResults.get("findings");
            long totalLines = countLinesOfCode(directoryPath);
            double warningDensity = findings.stream()
                    .filter(f -> "warning".equals(((String) f.get("severity")).toLowerCase()))
                    .count() / (double)totalLines;
            
            if (warningDensity < 0.01) { // Less than 1 warning per 100 lines
                bonus += CLEAN_CODE_BONUS;
            }

            // Documentation bonus
            if (hasGoodDocumentation(directoryPath)) {
                bonus += DOCUMENTATION_BONUS;
            }
        } catch (IOException e) {
            log.error("Error calculating bonus: {}", e.getMessage());
        }

        return bonus;
    }

    private boolean hasGoodTestCoverage(String directoryPath) {
        try {
            long testFiles = Files.walk(Paths.get(directoryPath))
                    .filter(path -> path.toString().endsWith("Test.java"))
                    .count();
            long sourceFiles = Files.walk(Paths.get(directoryPath))
                    .filter(path -> path.toString().endsWith(".java"))
                    .count();
            return testFiles >= sourceFiles * 0.5; // At least 50% test coverage
        } catch (IOException e) {
            log.error("Error checking test coverage: {}", e.getMessage());
            return false;
        }
    }

    private boolean hasGoodDocumentation(String directoryPath) {
        try {
            return Files.walk(Paths.get(directoryPath))
                    .filter(path -> path.toString().endsWith(".java"))
                    .allMatch(this::hasDocumentation);
        } catch (IOException e) {
            log.error("Error checking documentation: {}", e.getMessage());
            return false;
        }
    }

    private boolean hasDocumentation(Path path) {
        try {
            return Files.lines(path)
                    .anyMatch(line -> line.contains("/**") || line.contains("/*"));
        } catch (IOException e) {
            log.error("Error checking documentation in file {}: {}", path, e.getMessage());
            return false;
        }
    }

    private List<String> generateRecommendations(List<Map<String, Object>> findings) {
        List<String> recommendations = new ArrayList<>();
        Map<String, Integer> issueTypes = new HashMap<>();

        // Count issue types
        for (Map<String, Object> finding : findings) {
            String checkId = (String) finding.get("check_id");
            issueTypes.merge(checkId, 1, Integer::sum);
        }

        // Generate recommendations based on most common issues
        issueTypes.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> {
                String recommendation = generateRecommendationForIssue(entry.getKey(), entry.getValue());
                if (recommendation != null) {
                    recommendations.add(recommendation);
                }
            });

        return recommendations;
    }

    private String generateRecommendationForIssue(String checkId, int count) {
        // Add specific recommendations based on issue type
        if (checkId.contains("hardcoded-credentials")) {
            return "Move " + count + " hardcoded credentials to secure configuration or environment variables";
        } else if (checkId.contains("resource-leak")) {
            return "Fix " + count + " resource leaks by properly closing resources using try-with-resources";
        } else if (checkId.contains("sql-injection")) {
            return "Prevent " + count + " potential SQL injections by using prepared statements";
        } else if (checkId.contains("unsafe-ssl")) {
            return "Fix " + count + " unsafe SSL/TLS configurations by using proper certificate validation";
        }
        return null;
    }
} 