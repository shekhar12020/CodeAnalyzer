package com.codeanalyzer.service.analysis.impl;

import com.codeanalyzer.service.analysis.SemgrepRunnerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Slf4j
@Service
public class SemgrepRunnerServiceImpl implements SemgrepRunnerService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> runAnalysis(String directoryPath) {
        try {
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

            JsonNode jsonOutput = objectMapper.readTree(output.toString());
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

        } catch (Exception e) {
            log.error("Error running Semgrep analysis: {}", e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to run Semgrep analysis: " + e.getMessage());
            return errorResult;
        }
    }
} 