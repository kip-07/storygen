# StoryGen - LLM Story-to-HTML Generator

A Spring Boot application that accepts a prompt via REST API, generates a story using NVIDIA NIM, and converts it into a structured HTML document.

## Prerequisites

- **Java 21** or later
- **Maven 3.8+**
- **NVIDIA NIM API key** from [build.nvidia.com](https://build.nvidia.com/)

## Quick Start

```bash
cd storygen

# Build
mvn clean package -DskipTests

# Set your API key
export NVIDIA_API_KEY="nvapi-..."

# Run
java -jar target/storygen-0.0.1-SNAPSHOT.jar
```

Or use the run script:
```bash
./run.sh
```

The application starts on **http://localhost:8080**.

## API Usage

### Generate a Story

```bash
curl -X POST http://localhost:8080/api/stories \
  -H "Content-Type: application/json" \
  -d '{"prompt": "a lighthouse keeper who talks to the sea"}'
```

**Response:**
```json
{
  "id": "story_8f3a1c2d",
  "title": "The Keeper's Tide",
  "htmlContent": "<!DOCTYPE html>...",
  "downloadUrl": "/api/stories/story_8f3a1c2d/download"
}
```

### Download Generated HTML

```bash
curl -O http://localhost:8080/api/stories/story_8f3a1c2d/download
```

## Frontend

Visit **http://localhost:8080/** in your browser:
- Enter a story prompt
- Click "Generate Story"
- Preview the HTML in the iframe
- Download the generated HTML file

## Configuration

`src/main/resources/application.yml`:

```yaml
server:
  port: 8080

llm:
  nvidia:
    base-url: https://integrate.api.nvidia.com/v1
    api-key: ${NVIDIA_API_KEY:}
    model: nvidia/nemotron-3-ultra-550b-a55b
    max-tokens: 16384

storage:
  directory: ./generated
```

## Testing

```bash
mvn test
```

## HTML Conversion Rules

Deterministic conversion (no LLM call):

1. **Title:** First line (≤120 chars) becomes `<h1>`; otherwise "Untitled Story"
2. **Paragraphs:** Blank lines separate paragraphs; HTML characters are escaped
3. **Dialogue:** Paragraphs wholly wrapped in `"..."` get `class="dialogue"`
4. **Scene Breaks:** Lines with `***`, `---`, or `###` become `<hr class="scene-break">`

## License

MIT
