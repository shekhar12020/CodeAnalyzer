package com.codeanalyzer.service.analysis.impl;

import com.codeanalyzer.service.analysis.LanguageDetectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class LanguageDetectorServiceImpl implements LanguageDetectorService {

    private static final Map<String, List<String>> LANGUAGE_EXTENSIONS = new HashMap<>();
    
    static {
        // Java
        LANGUAGE_EXTENSIONS.put("java", Arrays.asList(".java"));
        
        // Python
        LANGUAGE_EXTENSIONS.put("python", Arrays.asList(".py", ".pyc", ".pyd", ".pyo", ".pyw", ".pyz"));
        
        // JavaScript/TypeScript
        LANGUAGE_EXTENSIONS.put("javascript", Arrays.asList(".js", ".jsx", ".ts", ".tsx"));
        
        // C/C++
        LANGUAGE_EXTENSIONS.put("cpp", Arrays.asList(".c", ".cpp", ".cc", ".cxx", ".h", ".hpp", ".hxx"));
        
        // Go
        LANGUAGE_EXTENSIONS.put("go", Arrays.asList(".go"));
        
        // Ruby
        LANGUAGE_EXTENSIONS.put("ruby", Arrays.asList(".rb", ".rbw"));
        
        // PHP
        LANGUAGE_EXTENSIONS.put("php", Arrays.asList(".php", ".phtml", ".php3", ".php4", ".php5", ".php7"));
    }

    @Override
    public Map<String, Object> detectLanguage(String path) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Integer> languageCounts = new HashMap<>();
        
        try {
            Path directoryPath = Paths.get(path);
            if (!Files.exists(directoryPath)) {
                throw new IllegalArgumentException("Path does not exist: " + path);
            }

            // Count files by extension
            Files.walk(directoryPath)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    String extension = getFileExtension(file.toString());
                    for (Map.Entry<String, List<String>> entry : LANGUAGE_EXTENSIONS.entrySet()) {
                        if (entry.getValue().contains(extension)) {
                            languageCounts.merge(entry.getKey(), 1, Integer::sum);
                        }
                    }
                });

            // Determine primary language
            String primaryLanguage = languageCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

            // Calculate confidence based on file distribution
            int totalFiles = languageCounts.values().stream().mapToInt(Integer::intValue).sum();
            double confidence = totalFiles > 0 ? 
                (double) languageCounts.getOrDefault(primaryLanguage, 0) / totalFiles : 0.0;

            // Prepare result
            result.put("language", primaryLanguage);
            result.put("confidence", confidence);
            result.put("file_counts", languageCounts);
            result.put("total_files", totalFiles);

        } catch (IOException e) {
            log.error("Error detecting language: {}", e.getMessage());
            result.put("error", "Failed to detect language: " + e.getMessage());
        }

        return result;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex).toLowerCase() : "";
    }
} 