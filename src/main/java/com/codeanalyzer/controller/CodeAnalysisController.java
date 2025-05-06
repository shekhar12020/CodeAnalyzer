package com.codeanalyzer.controller;

import com.codeanalyzer.service.CodeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class CodeAnalysisController {

    private final CodeAnalysisService codeAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeCode(@RequestBody Map<String, String> request) {
        String directoryPath = request.get("directoryPath");
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Map<String, Object> analysisResults = codeAnalysisService.analyzeCode(directoryPath);
        return ResponseEntity.ok(analysisResults);
    }
} 