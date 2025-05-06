# Code Analyzer Tool

A Spring Boot application that performs static code analysis using multiple tools to provide comprehensive code quality assessment.

## Features

- Static code analysis using multiple tools:
  - Semgrep CLI for security and bug detection
  - PMD for code quality analysis
  - Checkstyle for code style checking
  - SpotBugs for bug detection
- REST API for code analysis
- Quality score calculation based on multiple metrics

## Prerequisites

- Java 17 or higher
- Gradle 7.x or higher
- Semgrep CLI (required for code analysis)

### Installing Semgrep CLI

#### macOS
```bash
brew install semgrep
```

#### Linux
```bash
python3 -m pip install semgrep
```

#### Windows
```bash
python3 -m pip install semgrep
```

For more installation options, visit: https://semgrep.dev/docs/getting-started/

## Building the Project

```bash
./gradlew build
```

## Running the Application

```bash
./gradlew bootRun
```

## API Usage

### Analyze Code

```http
POST /api/analysis/analyze
Content-Type: application/json

{
    "directoryPath": "/path/to/your/code"
}
```

Response:
```json
{
    "qualityScore": 85.5,
    "semgrepResults": {
        "findings": [...],
        "severity": {
            "error": 0,
            "warning": 5,
            "info": 10
        }
    },
    "pmdResults": { ... },
    "checkstyleResults": { ... },
    "spotBugsResults": { ... }
}
```

## Configuration

The application can be configured through `application.properties` file. Key configurations include:

- Server port
- Logging levels
- File upload limits

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request 