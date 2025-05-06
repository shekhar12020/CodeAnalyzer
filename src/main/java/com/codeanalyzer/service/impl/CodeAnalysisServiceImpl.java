package com.codeanalyzer.service.impl;

import com.codeanalyzer.service.CodeAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
public class CodeAnalysisServiceImpl implements CodeAnalysisService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Pattern jsonStartPattern = Pattern.compile("\\{\\s*\"");
    
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
    public Map<String, Object> analyzeCode(String directoryPath) {
        try {
            Map<String, Object> semgrepResults = runSemgrepAnalysis(directoryPath);
            double qualityScore = calculateQualityScore(semgrepResults, directoryPath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("qualityScore", qualityScore);
            response.put("semgrepResults", semgrepResults);
            
            return response;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Analysis failed: " + e.getMessage());
            errorResponse.put("qualityScore", 0.0);
            return errorResponse;
        }
    }
    
    private Map<String, Object> runSemgrepAnalysis(String directoryPath) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "semgrep",
            "scan",
            "--config",
            "auto",
            "--config",
            ".semgrep/rules/java-security.yml",
            "--json",
            directoryPath
        );

        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonOutput = mapper.readTree(output.toString());
        
        Map<String, Object> results = new HashMap<>();
        results.put("exitCode", exitCode);
        results.put("findings", new ArrayList<>());
        results.put("severity_counts", new HashMap<String, Integer>());
        results.put("total_findings", 0);
        results.put("scan_time", 0.0);
        results.put("full_output", output.toString());

        if (exitCode != 0) {
            results.put("error", "Semgrep analysis failed with exit code: " + exitCode);
            return results;
        }

        JsonNode findings = jsonOutput.path("results");
        List<Map<String, Object>> processedFindings = new ArrayList<>();
        Map<String, Integer> severityCounts = new HashMap<>();
        severityCounts.put("error", 0);
        severityCounts.put("warning", 0);
        severityCounts.put("info", 0);

        for (JsonNode finding : findings) {
            Map<String, Object> processedFinding = new HashMap<>();
            processedFinding.put("check_id", finding.path("check_id").asText());
            processedFinding.put("path", finding.path("path").asText());
            processedFinding.put("start_line", finding.path("start").path("line").asInt());
            processedFinding.put("end_line", finding.path("end").path("line").asInt());
            processedFinding.put("message", finding.path("extra").path("message").asText());
            
            String severity = finding.path("extra").path("severity").asText().toLowerCase();
            processedFinding.put("severity", severity);
            
            Map<String, String> metadata = new HashMap<>();
            JsonNode metadataNode = finding.path("extra").path("metadata");
            metadata.put("category", metadataNode.path("category").asText());
            metadata.put("confidence", metadataNode.path("confidence").asText());
            metadata.put("impact", metadataNode.path("impact").asText());
            metadata.put("cwe", metadataNode.path("cwe").asText());
            metadata.put("owasp", metadataNode.path("owasp").asText());
            processedFinding.put("metadata", metadata);

            processedFindings.add(processedFinding);
            severityCounts.put(severity, severityCounts.getOrDefault(severity, 0) + 1);
        }

        results.put("findings", processedFindings);
        results.put("severity_counts", severityCounts);
        results.put("total_findings", processedFindings.size());

        return results;
    }
    
    private double calculateQualityScore(Map<String, Object> semgrepResults, String directoryPath) {
        if (semgrepResults == null || !semgrepResults.containsKey("findings")) {
            return 0.0;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) semgrepResults.get("findings");
        if (findings.isEmpty()) {
            return BASE_SCORE;
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
        double bonus = calculateBonus(semgrepResults, directoryPath);
        score += bonus;

        // Ensure score is within bounds
        return Math.max(0.0, Math.min(100.0, score));
    }

    private long countLinesOfCode(String directoryPath) {
        try {
            return Files.walk(Paths.get(directoryPath))
                    .filter(path -> path.toString().endsWith(".java"))
                    .mapToLong(this::countLinesInFile)
                    .sum();
        } catch (Exception e) {
            log.error("Error counting lines of code: {}", e.getMessage());
            return 1000; // Default to 1000 if we can't count
        }
    }

    private long countLinesInFile(Path path) {
        try {
            return Files.lines(path).count();
        } catch (Exception e) {
            log.error("Error counting lines in file {}: {}", path, e.getMessage());
            return 0;
        }
    }

    private double calculateBonus(Map<String, Object> semgrepResults, String directoryPath) {
        double bonus = 0.0;

        // Test coverage bonus
        if (hasGoodTestCoverage(directoryPath)) {
            bonus += TEST_COVERAGE_BONUS;
        }

        // Clean code bonus (fewer warnings relative to code size)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> findings = (List<Map<String, Object>>) semgrepResults.get("findings");
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
        } catch (Exception e) {
            log.error("Error checking test coverage: {}", e.getMessage());
            return false;
        }
    }

    private boolean hasGoodDocumentation(String directoryPath) {
        try {
            return Files.walk(Paths.get(directoryPath))
                    .filter(path -> path.toString().endsWith(".java"))
                    .allMatch(this::hasDocumentation);
        } catch (Exception e) {
            log.error("Error checking documentation: {}", e.getMessage());
            return false;
        }
    }

    private boolean hasDocumentation(Path path) {
        try {
            return Files.lines(path)
                    .anyMatch(line -> line.contains("/**") || line.contains("/*"));
        } catch (Exception e) {
            log.error("Error checking documentation in file {}: {}", path, e.getMessage());
            return false;
        }
    }
} 