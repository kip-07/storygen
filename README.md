# StoryGen

LLM Story-to-HTML Generator - A Spring Boot application that generates stories using NVIDIA NIM and converts them to structured HTML documents.

---

## Prerequisites

| Requirement | Version | Check Command |
|-------------|---------|---------------|
| Java | 21+ | `java --version` |
| Maven | 3.8+ | `mvn --version` |
| NVIDIA API Key | - | Get one at [build.nvidia.com](https://build.nvidia.com/) |

### Installing Java 21

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

**macOS:**
```bash
brew install openjdk@21
```

**Windows:**
Download from [Adoptium](https://adoptium.net/)

### Installing Maven

**Ubuntu/Debian:**
```bash
sudo apt install maven
```

**macOS:**
```bash
brew install maven
```

**Windows:**
Download from [maven.apache.org](https://maven.apache.org/download.cgi)

---

## Setup

### 1. Clone the Project

```bash
git clone <repository-url>
cd storygen
```

### 2. Get NVIDIA API Key

1. Go to [build.nvidia.com](https://build.nvidia.com/)
2. Sign up / Log in
3. Click on any model (e.g., Nemotron)
4. Click "Get API Key"
5. Copy the key (starts with `nvapi-`)

### 3. Configure API Key

```bash
cp .env.example .env
```

Edit `.env` and replace with your actual key:
```
NVIDIA_API_KEY=nvapi-your-actual-key-here
```

**Never commit `.env` to git (it's already in `.gitignore`)**

---

## Build

```bash
mvn clean package -DskipTests
```

This compiles the code and creates `target/storygen-0.0.1-SNAPSHOT.jar`

**Expected output:**
```
[INFO] BUILD SUCCESS
```

---

## Run

### Option 1: Run Script (Recommended)

```bash
./run.sh
```

This automatically:
- Kills any existing instance
- Starts the application
- Cleans up on exit

### Option 2: Direct

```bash
java -jar target/storygen-0.0.1-SNAPSHOT.jar
```

### Option 3: With Maven

```bash
mvn spring-boot:run
```

---

## Verify It's Working

**Terminal should show:**
```
Started StorygenApplication in X.X seconds
Tomcat started on port 8080
```

**Test with curl:**
```bash
curl http://localhost:8080/
```

Should return HTML content.

---

## Usage

### Web Interface

1. Open browser: **http://localhost:8080/**
2. Enter a story prompt (e.g., "a robot who dreams of becoming a chef")
3. Click "Generate Story"
4. Preview the generated HTML
5. Download the HTML file

### API

**Generate a story:**
```bash
curl -X POST http://localhost:8080/api/stories \
  -H "Content-Type: application/json" \
  -d '{"prompt": "a lighthouse keeper who talks to the sea"}'
```

**Response:**
```json
{
  "id": "story_8f3a1c2d",
  "title": "The Keeper's Light",
  "htmlContent": "<!DOCTYPE html>...",
  "downloadUrl": "/api/stories/story_8f3a1c2d/download"
}
```

**Download the HTML file:**
```bash
curl -O http://localhost:8080/api/stories/story_8f3a1c2d/download
```

---

## Project Structure

```
storygen/
├── src/
│   ├── main/
│   │   ├── java/com/storygen/
│   │   │   ├── StorygenApplication.java      # Entry point
│   │   │   ├── controller/
│   │   │   │   ├── StoryController.java       # REST endpoints
│   │   │   │   └── GlobalExceptionHandler.java # Error handling
│   │   │   ├── dto/
│   │   │   │   ├── StoryRequest.java          # Input DTO
│   │   │   │   ├── StoryResponse.java         # Output DTO
│   │   │   │   └── HtmlDocument.java          # Internal DTO
│   │   │   ├── llm/
│   │   │   │   ├── LlmClient.java             # Interface
│   │   │   │   ├── NvidiaNimLlmClient.java    # NVIDIA implementation
│   │   │   │   ├── LlmClientConfig.java       # Configuration
│   │   │   │   └── LlmException.java          # Custom exception
│   │   │   ├── convert/
│   │   │   │   ├── StoryToHtmlConverter.java  # Text → HTML elements
│   │   │   │   └── HtmlDocumentBuilder.java   # Full HTML assembly
│   │   │   └── storage/
│   │   │       └── FileStorageService.java    # File I/O
│   │   └── resources/
│   │       ├── application.yml                 # Configuration
│   │       └── static/
│   │           ├── index.html                  # Frontend
│   │           ├── app.js                      # JavaScript
│   │           └── style.css                   # Styles
│   └── test/                                   # Unit & integration tests
├── target/                                     # Build output
├── .env.example                                # Environment template
├── .env                                        # Your secrets (gitignored)
├── run.sh                                      # Run script
├── pom.xml                                     # Maven config
└── REFERENCE.md                                # Technical documentation
```

---

## Testing

### Run All Tests

```bash
mvn test
```

**Expected output:**
```
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Test Coverage

| Test Class | Tests | What It Tests |
|------------|-------|---------------|
| `StoryToHtmlConverterTest` | 10 | HTML conversion rules |
| `NvidiaNimLlmClientTest` | 4 | API response parsing |
| `StoryControllerIntegrationTest` | 5 | REST endpoints |

---

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080                    # Change port if needed

llm:
  nvidia:
    base-url: https://integrate.api.nvidia.com/v1
    api-key: ${NVIDIA_API_KEY}  # Reads from .env
    model: nvidia/nemotron-3-ultra-550b-a55b
    max-tokens: 16384           # Max story length

storage:
  directory: ./generated        # Where HTML files are saved
```

---

## Troubleshooting

### "Port already in use"

Kill existing instance:
```bash
pkill -f storygen
```

### "NVIDIA_API_KEY must be set"

Check your `.env` file:
```bash
cat .env
```

Ensure it contains:
```
NVIDIA_API_KEY=nvapi-your-key-here
```

### "503 SERVICE_UNAVAILABLE"

NVIDIA API rate limit. Wait 10-30 seconds and try again.

### Build fails

Clean and rebuild:
```bash
mvn clean package -DskipTests
```

### Application won't start

Check Java version:
```bash
java --version
```
Must be Java 21 or higher.

---

## Useful Commands

```bash
# Build
mvn clean package -DskipTests

# Run
./run.sh

# Run tests
mvn test

# Check running processes
ps aux | grep storygen

# Kill the app
pkill -f storygen

# View logs (if running in background)
tail -f /tmp/storygen.log
```

---

## License

MIT
