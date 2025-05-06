package com.codeanalyzer.service.analysis;

import java.util.Map;

public interface LanguageDetectorService {
    /**
     * Detects the programming language of the given file or directory
     * @param path Path to the file or directory to analyze
     * @return Map containing language information including:
     *         - language: primary language detected
     *         - confidence: confidence score of detection
     *         - additional_languages: other languages found if any
     */
    Map<String, Object> detectLanguage(String path);
} 