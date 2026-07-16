# StoryGen - Complete Technical Reference Document

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Task Requirements Mapping](#2-task-requirements-mapping)
3. [Tech Stack Deep Dive](#3-tech-stack-deep-dive)
4. [Architecture & Design](#4-architecture--design)
5. [Code Walkthrough - Every File](#5-code-walkthrough---every-file)
6. [Configuration Deep Dive](#6-configuration-deep-dive)
7. [Frontend (JavaScript)](#7-frontend-javascript)
8. [Testing Strategy](#8-testing-strategy)
9. [Build & Deployment](#9-build--deployment)
10. [Security Considerations](#10-security-considerations)
11. [API Documentation](#11-api-documentation)
12. [Flow Diagrams](#12-flow-diagrams)

---

## 1. Project Overview

**What it does:**
A web application that takes a story prompt (e.g., "a lighthouse keeper who talks to the sea"), sends it to NVIDIA's NIM LLM API, receives a generated story, converts it into a properly structured HTML document, and saves it to disk.

**Key Technologies:**
- Java 21
- Spring Boot 3.2
- Maven (build tool)
- NVIDIA NIM API (LLM provider)
- Vanilla JavaScript (frontend)

---

## 2. Task Requirements Mapping

| Task Step | Requirement | Implementation |
|-----------|-------------|----------------|
| 1. Send request to LLM API | HTTP POST to LLM with prompt | `NvidiaNimLlmClient.java` → calls NVIDIA NIM `/chat/completions` endpoint |
| 2. Receive story as plain text | Parse API response | Extract `choices[0].message.content` from JSON response |
| 3. Process in Java | Parse, transform text | `StoryToHtmlConverter.java` applies deterministic formatting rules |
| 4. Convert to structured HTML | Generate valid HTML5 document | `HtmlDocumentBuilder.java` wraps content in `<!DOCTYPE html>` template |
| 5. Link external CSS (optional) | Optional styling | **Removed per user request** - no CSS linking |
| 6. Save/return HTML | Store file, return response | `FileStorageService.java` saves to `./generated/{id}.html`, returns JSON |

---

## 3. Tech Stack Deep Dive

### 3.1 Java 21

**What is it:**
Java 21 is the latest Long-Term Support (LTS) release of the Java programming language (released September 2023).

**Why Java:**
- Strongly typed - catches errors at compile time
- Platform independent - "Write Once, Run Anywhere" via JVM
- Mature ecosystem - Spring Boot, Maven, extensive libraries
- Enterprise standard - widely used in production systems

**Key Java 21 Features Used:**
- **Text Blocks (Java 13+):** Multi-line strings with `"""` syntax
  ```java
  String html = """
      <html>
          <body>Hello</body>
      </html>
      """;
  ```
- **Records (Java 14+):** Immutable data classes
  ```java
  public record StoryRequest(String prompt) {}
  // Auto-generates constructor, getters, equals, hashCode, toString
  ```

### 3.2 Spring Boot 3.2

**What is Spring Boot:**
A framework that simplifies creating standalone, production-grade Spring applications. It provides:
- Auto-configuration (reduces boilerplate)
- Embedded web server (no need to deploy WAR files)
- Dependency management
- Production-ready features (health checks, metrics)

**What is Spring:**
A Java framework for enterprise application development. It provides:
- **IoC (Inversion of Control):** Objects don't create their dependencies; the framework injects them
- **DI (Dependency Injection):** Automatic wiring of components
- **AOP (Aspect-Oriented Programming):** Cross-cutting concerns (logging, security)

**Why Spring Boot:**
- Reduces development time significantly
- Convention over configuration
- Production-ready out of the box
- Massive community and documentation

### 3.3 Embedded Tomcat Server

**What is Tomcat:**
Apache Tomcat is an open-source implementation of the Java Servlet, JavaServer Pages, Java Expression Language, and WebSocket technologies. It's a web server and servlet container.

**What is Embedded Tomcat:**
Instead of installing Tomcat separately and deploying your application to it, Spring Boot includes Tomcat *inside* your application. When you run `java -jar app.jar`, Tomcat starts automatically as part of your application.

**Why Embedded Tomcat:**
- **No external server needed:** Just run `java -jar` and it works
- **Portable:** The entire application (including server) is one file
- **Simpler deployment:** No need to configure Tomcat, just run the JAR
- **Development speed:** Start/stop instantly during development

**Traditional vs Embedded:**
```
Traditional:
  [Your App] → deploy to → [External Tomcat] → serves to → [Users]

Embedded:
  [Your App + Tomcat] → serves to → [Users]
```

### 3.4 Maven

**What is Maven:**
A build automation and dependency management tool for Java projects.

**What it does:**
1. **Dependency Management:** Automatically downloads required libraries (Spring, WebClient, etc.)
2. **Build Lifecycle:** Compile → Test → Package → Install
3. **Project Structure:** Enforces standard directory layout

**pom.xml (Project Object Model):**
The configuration file that defines:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>
```
- **parent:** Inherits Spring Boot's default configurations
- **groupId:** Your organization's identifier
- **artifactId:** Your project's identifier
- **version:** Project version

**Key Maven Commands:**
```bash
mvn clean package        # Compile, test, create JAR
mvn clean package -DskipTests  # Skip tests during build
mvn test                 # Run tests only
```

### 3.5 WebClient (Spring WebFlux)

**What is WebClient:**
A non-blocking, reactive HTTP client introduced in Spring 5. It's used to make HTTP requests to external services.

**Why WebClient (not RestTemplate):**
- **Non-blocking:** Doesn't wait idle for responses (better resource utilization)
- **Reactive:** Supports reactive streams (Flux/Mono)
- **Modern:** RestTemplate is deprecated in favor of WebClient
- **Better timeout handling:** Built-in timeout support

**How it's used:**
```java
WebClient webClient = WebClient.builder()
    .baseUrl("https://integrate.api.nvidia.com/v1")
    .defaultHeader("Authorization", "Bearer " + apiKey)
    .build();

String response = webClient.post()
    .uri("/chat/completions")
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(requestBody)
    .retrieve()
    .bodyToMono(String.class)
    .timeout(Duration.ofSeconds(60))
    .block();  // Block until response (simplified for this use case)
```

### 3.6 NVIDIA NIM API

**What is NVIDIA NIM:**
NVIDIA Inference Microservices - a cloud API for running AI models. It provides access to various LLMs including Nemotron, Llama, etc.

**OpenAI-Compatible Format:**
NVIDIA NIM uses the same API format as OpenAI, making it easy to switch providers:

**Request:**
```json
{
  "model": "nvidia/nemotron-3-ultra-550b-a55b",
  "messages": [
    {"role": "user", "content": "Write a story about..."}
  ],
  "temperature": 1.0,
  "max_tokens": 16384
}
```

**Response:**
```json
{
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "Once upon a time..."
      }
    }
  ]
}
```

### 3.7 Jakarta Validation

**What is it:**
Jakarta Validation (formerly Bean Validation) provides a standard way to validate Java objects using annotations.

**How it's used:**
```java
public record StoryRequest(
    @NotBlank(message = "prompt must not be blank")
    @Size(max = 2000, message = "prompt must not exceed 2000 characters")
    String prompt
) {}
```

- `@NotBlank`: Ensures string is not null, empty, or whitespace-only
- `@Size`: Validates string length

**Why:**
- Declarative validation (no manual if-statements)
- Consistent across application
- Automatic error messages

### 3.8 SLF4J (Logging)

**What is it:**
Simple Logging Facade for Java - a logging abstraction that allows you to plug in different logging implementations.

**How it's used:**
```java
private static final Logger log = LoggerFactory.getLogger(StoryController.class);

log.info("Saved HTML document to {}", filePath);
log.error("Failed to save HTML", exception);
```

**Why:**
- Decouples code from specific logging framework
- Can switch implementations without changing code
- Standard in Java ecosystem

---

## 4. Architecture & Design

### 4.1 Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                      │
│  StoryController (REST API endpoints)                       │
│  GlobalExceptionHandler (error handling)                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                          │
│  LlmClient (interface for LLM providers)                    │
│  StoryToHtmlConverter (text → HTML conversion)              │
│  HtmlDocumentBuilder (HTML5 document assembly)              │
│  FileStorageService (file I/O)                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     EXTERNAL SERVICES                       │
│  NVIDIA NIM API (LLM inference)                             │
│  File System (storage)                                      │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Dependency Injection

**What is DI:**
A design pattern where objects don't create their dependencies; instead, they receive them from an external source (the Spring container).

**Without DI:**
```java
public class StoryController {
    private LlmClient llmClient = new NvidiaNimLlmClient(...); // Tight coupling
    private StoryToHtmlConverter converter = new StoryToHtmlConverter();
}
```

**With DI (Spring):**
```java
public class StoryController {
    private final LlmClient llmClient;  // Interface, not implementation
    private final StoryToHtmlConverter converter;

    // Spring injects the dependencies
    public StoryController(LlmClient llmClient, StoryToHtmlConverter converter) {
        this.llmClient = llmClient;
        this.converter = converter;
    }
}
```

**Benefits:**
- **Loose coupling:** Controller doesn't know which LLM implementation is used
- **Testability:** Easy to inject mocks for testing
- **Flexibility:** Switch implementations without changing controller code

### 4.3 Interface-Based Design

**LlmClient Interface:**
```java
public interface LlmClient {
    String generateStory(String prompt);
}
```

**Why an interface:**
- **Abstraction:** Controller only knows it can call `generateStory()`
- **Multiple implementations:** Could add OpenAI, Anthropic, etc.
- **Testability:** Can mock the interface in tests

### 4.4 Separation of Concerns

| Class | Responsibility |
|-------|----------------|
| `StoryController` | HTTP request handling, response formatting |
| `NvidiaNimLlmClient` | LLM API communication |
| `StoryToHtmlConverter` | Text parsing, HTML element generation |
| `HtmlDocumentBuilder` | Full HTML document assembly |
| `FileStorageService` | File I/O, ID generation, validation |
| `GlobalExceptionHandler` | Centralized error handling |

---

## 5. Code Walkthrough - Every File

### 5.1 StorygenApplication.java

```java
@SpringBootApplication
public class StorygenApplication {
    public static void main(String[] args) {
        SpringApplication.run(StorygenApplication.class, args);
    }
}
```

**@SpringBootApplication** is a combination of:
- `@Configuration`: Marks this class as a configuration source
- `@EnableAutoConfiguration`: Enables Spring Boot's auto-configuration
- `@ComponentScan`: Scans for components (controllers, services, etc.)

**SpringApplication.run():**
1. Creates the Spring application context
2. Starts the embedded Tomcat server
3. Scans for and registers all Spring components
4. Starts the application

### 5.2 LlmClient.java (Interface)

```java
public interface LlmClient {
    String generateStory(String prompt);
}
```

**Why an interface:**
- Defines a contract: any LLM client must implement `generateStory()`
- Enables polymorphism: can switch between LLM providers
- Simplifies testing: mock the interface

### 5.3 NvidiaNimLlmClient.java

**Constructor:**
```java
public NvidiaNimLlmClient(String baseUrl, String apiKey, String model, int maxTokens) {
    this.model = model;
    this.maxTokens = maxTokens;
    this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
}
```

**What happens:**
1. Stores model and maxTokens configuration
2. Creates a WebClient with base URL and authentication headers
3. The `Authorization: Bearer {apiKey}` header authenticates with NVIDIA

**generateStory() method:**
```java
Map<String, Object> requestBody = Map.of(
    "model", model,
    "messages", List.of(
        Map.of("role", "user", "content", prompt)
    ),
    "temperature", 1.0,
    "top_p", 0.95,
    "max_tokens", maxTokens,
    "stream", false
);
```

**Request parameters:**
- `model`: Which LLM to use (nemotron-3-ultra-550b-a55b)
- `messages`: Array of conversation messages (user prompt)
- `temperature`: Randomness (1.0 = balanced)
- `top_p`: Nucleus sampling (0.95 = consider top 95% probability mass)
- `max_tokens`: Maximum response length
- `stream`: false = wait for complete response (not streaming)

**extractResponseText() method:**
```java
List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
Map<String, Object> firstChoice = choices.get(0);
Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
String text = (String) message.get("content");
```

**Parses OpenAI-compatible response:**
```
Response JSON structure:
{
  "choices": [
    {
      "message": {
        "content": "The actual story text..."
      }
    }
  ]
}
```

### 5.4 LlmClientConfig.java

```java
@Configuration
public class LlmClientConfig {
    @Bean
    public LlmClient llmClient(
            @Value("${llm.nvidia.base-url}") String baseUrl,
            @Value("${llm.nvidia.api-key}") String apiKey,
            @Value("${llm.nvidia.model}") String model,
            @Value("${llm.nvidia.max-tokens:16384}") int maxTokens) {
        
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("llm.nvidia.api-key must be set");
        }
        return new NvidiaNimLlmClient(baseUrl, apiKey, model, maxTokens);
    }
}
```

**@Configuration:** Marks this class as a source of bean definitions

**@Bean:** Registers a method return value as a Spring bean (managed object)

**@Value:** Injects values from `application.yml`
- `${llm.nvidia.base-url}` → reads `llm.nvidia.base-url` property
- `${llm.nvidia.max-tokens:16384}` → reads property with default value 16384

**Why this pattern:**
- Separates configuration from implementation
- Makes the LLM client configurable via properties
- Validates API key at startup (fail-fast)

### 5.5 StoryRequest.java

```java
public record StoryRequest(
    @NotBlank(message = "prompt must not be blank")
    @Size(max = 2000, message = "prompt must not exceed 2000 characters")
    String prompt
) {}
```

**Java Record:**
- Immutable data class
- Auto-generates: constructor, getters, equals(), hashCode(), toString()
- No boilerplate code

**Validation Annotations:**
- `@NotBlank`: Rejects null, empty, or whitespace-only strings
- `@Size(max=2000)`: Limits prompt length to prevent abuse

### 5.6 StoryResponse.java

```java
public record StoryResponse(String id, String title, String htmlContent, String downloadUrl) {}
```

**Response structure:**
- `id`: Unique identifier (e.g., "story_8f3a1c2d")
- `title`: Extracted or generated title
- `htmlContent`: The complete HTML document
- `downloadUrl`: API endpoint to download the HTML file

### 5.7 HtmlDocument.java

```java
public record HtmlDocument(String title, String bodyHtml) {}
```

**Intermediate representation:**
- `title`: Story title for the `<h1>` tag
- `bodyHtml`: The formatted body content (paragraphs, dialogue, scene breaks)

### 5.8 StoryToHtmlConverter.java

**Core conversion logic:**

```java
public HtmlDocument convert(String rawStory) {
    String[] lines = rawStory.split("\\n", -1);
    
    // Title extraction
    if (lines.length > 0 && lines[0].trim().length() <= 120) {
        title = escapeHtml(lines[0].trim());
        bodyStartIndex = 1;
    } else {
        title = "Untitled Story";
        bodyStartIndex = 0;
    }
    
    // Split into blocks by blank lines
    String[] blocks = BLANK_LINES.split(trimmedBody, -1);
    
    for (String block : blocks) {
        if (isSceneBreak(trimmedBlock)) {
            elements.add("<hr class=\"scene-break\">");
        } else if (isDialogue(trimmedBlock)) {
            elements.add("<p class=\"dialogue\">" + escapeHtml(trimmedBlock) + "</p>");
        } else {
            elements.add("<p>" + escapeHtml(trimmedBlock) + "</p>");
        }
    }
}
```

**Formatting Rules:**

| Pattern | HTML Output |
|---------|-------------|
| First line ≤ 120 chars | `<h1>{title}</h1>` |
| Blank line | Separates paragraphs |
| Line with `***`, `---`, or `###` | `<hr class="scene-break">` |
| Line wholly wrapped in `"..."` | `<p class="dialogue">` |
| All other lines | `<p>` |

**Why deterministic (no LLM for conversion):**
- Fast (no API call needed)
- Predictable (same input → same output)
- No additional cost
- Reliable (no LLM hallucination in formatting)

### 5.9 HtmlDocumentBuilder.java

```java
public String build(HtmlDocument doc) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>%s</title>
        </head>
        <body>
          <article>
            <h1>%s</h1>
        %s  </article>
        </body>
        </html>
        """.formatted(escapedTitle, escapedTitle, indentBody(doc.bodyHtml()));
}
```

**HTML5 Structure:**
- `<!DOCTYPE html>`: Declares HTML5 document type
- `<meta charset="UTF-8">`: Character encoding
- `<meta name="viewport">`: Responsive design
- `<title>`: Browser tab title
- `<article>`: Semantic HTML for content
- `<h1>`: Main heading

### 5.10 FileStorageService.java

```java
@Service
public class FileStorageService {
    private final Path storageDir;

    public FileStorageService(@Value("${storage.directory}") String storageDirectory) {
        this.storageDir = Paths.get(storageDirectory);
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(storageDir);
    }
```

**@Service:** Marks this as a Spring service component

**@PostConstruct:** Method runs after dependency injection is complete
- Creates storage directory if it doesn't exist

**ID Generation:**
```java
public String generateId() {
    return "story_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
}
```
- Generates: `story_8f3a1c2d`
- Format: `story_` + 8 random hex characters

**Path Traversal Protection:**
```java
private static final Pattern VALID_ID = Pattern.compile("^story_[a-z0-9]{8}$");

private void validateId(String id) {
    if (id == null || !VALID_ID.matcher(id).matches()) {
        throw new IllegalArgumentException("Invalid story ID: " + id);
    }
}
```
- Only allows exactly `story_` + 8 lowercase alphanumeric characters
- Prevents attacks like `../../etc/passwd`

### 5.11 StoryController.java

```java
@RestController
@RequestMapping("/api/stories")
public class StoryController {
```

**@RestController:** Combines `@Controller` + `@ResponseBody`
- Marks this class as handling HTTP requests
- Automatically serializes return values to JSON

**@RequestMapping("/api/stories"):** Base URL path for all endpoints

**POST endpoint:**
```java
@PostMapping
public ResponseEntity<StoryResponse> createStory(@Valid @RequestBody StoryRequest request) {
    String rawStory = llmClient.generateStory(request.prompt());
    HtmlDocument doc = converter.convert(rawStory);
    String html = documentBuilder.build(doc);
    
    String id = storageService.generateId();
    storageService.save(id, html);
    
    return ResponseEntity.ok(new StoryResponse(id, doc.title(), html, downloadUrl));
}
```

**Flow:**
1. `@RequestBody` deserializes JSON to `StoryRequest`
2. `@Valid` triggers validation annotations
3. Calls LLM to generate story
4. Converts to HTML
5. Builds full HTML document
6. Saves to file
7. Returns JSON response

**GET endpoint:**
```java
@GetMapping("/{id}/download")
public ResponseEntity<Resource> downloadStory(@PathVariable String id) throws IOException {
    Resource resource = storageService.load(id);
    return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + ".html\"")
            .body(resource);
}
```

**@PathVariable:** Extracts `{id}` from URL path
**Content-Disposition:** Triggers browser download dialog

### 5.12 GlobalExceptionHandler.java

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
```

**@RestControllerAdvice:** Applies to all controllers, catches exceptions

**Exception handlers:**
```java
@ExceptionHandler(LlmException.class)
public ResponseEntity<Map<String, String>> handleLlmException(LlmException ex) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of("error", ex.getMessage()));
}
```

**Maps exceptions to HTTP status codes:**
| Exception | HTTP Status | Meaning |
|-----------|-------------|---------|
| `LlmException` | 502 Bad Gateway | LLM API error |
| `MethodArgumentNotValidException` | 400 Bad Request | Validation failure |
| `IllegalArgumentException` | 404 Not Found | Invalid story ID |
| `Exception` | 500 Internal Server Error | Unexpected error |

---

## 6. Configuration Deep Dive

### 6.1 application.yml

```yaml
server:
  port: 8080

llm:
  nvidia:
    base-url: https://integrate.api.nvidia.com/v1
    api-key: ${NVIDIA_API_KEY}
    model: nvidia/nemotron-3-ultra-550b-a55b
    max-tokens: 16384

storage:
  directory: ./generated
```

**YAML Structure:**
- Hierarchical configuration (nested objects)
- `${NVIDIA_API_KEY}` references environment variable
- `${llm.nvidia.max-tokens:16384}` provides default value

**Why YAML over properties:**
- More readable for nested config
- Supports lists and maps natively
- Standard in Spring Boot

### 6.2 Environment Variables

**.env file:**
```
NVIDIA_API_KEY=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
```

**Why environment variables:**
- Secrets stay out of code (security)
- Different values per environment (dev/staging/prod)
- Standard practice in cloud deployments

**spring-dotenv library:**
- Automatically loads `.env` file into Spring's environment
- No need to `export` or `source` manually

### 6.3 .gitignore

```
.env              # Never commit secrets
target/           # Build output (regenerated)
generated/        # User data (not source code)
.idea/            # IDE-specific files
```

**Why ignore these:**
- `.env`: Contains API key (security risk)
- `target/`: Can be regenerated with `mvn package`
- `generated/`: User-generated content, not part of source

---

## 7. Frontend (JavaScript)

**Note:** JavaScript is used ONLY for the browser UI. All core logic is in Java.

### 7.1 index.html

```html
<form id="storyForm">
    <textarea id="prompt" required maxlength="2000"></textarea>
    <button type="submit" id="generateBtn">Generate Story</button>
</form>

<div id="result" style="display:none;">
    <iframe id="preview"></iframe>
    <a id="downloadLink">Download HTML</a>
</div>
```

**HTML5 Features:**
- `<form>` with validation (`required`, `maxlength`)
- `<iframe>` for HTML preview (sandboxed rendering)
- Progressive disclosure (result div hidden initially)

### 7.2 app.js

```javascript
document.getElementById('storyForm').addEventListener('submit', async function (e) {
    e.preventDefault();
    
    const response = await fetch('/api/stories', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: prompt })
    });
    
    const data = await response.json();
    preview.srcdoc = data.htmlContent;
    downloadLink.href = data.downloadUrl;
});
```

**Key concepts:**
- `e.preventDefault()`: Stops form from doing traditional HTML submit
- `fetch()`: Modern HTTP client (replaces XMLHttpRequest)
- `async/await`: Clean asynchronous code
- `srcdoc`: Sets iframe content directly from HTML string

### 7.3 style.css

**CSS Reset:**
```css
* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
}
```
Removes browser default styles for consistency.

**Layout:**
```css
.container {
    max-width: 720px;
    margin: 2rem auto;
}
```
Centers content with max-width for readability.

---

## 8. Testing Strategy

### 8.1 Unit Tests

**StoryToHtmlConverterTest:**
- Tests each formatting rule independently
- Verifies title extraction, paragraph splitting, dialogue detection, scene breaks
- Tests edge cases (empty story, long titles, XSS prevention)

### 8.2 Integration Tests

**StoryControllerIntegrationTest:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class StoryControllerIntegrationTest {
    @MockBean
    private LlmClient llmClient;
    
    @Autowired
    private MockMvc mockMvc;
}
```

**What this tests:**
- Full request/response cycle
- Validation (blank prompts)
- Path traversal protection
- Download endpoint

**@MockBean:**
- Replaces the real `LlmClient` with a mock
- Returns canned responses (no actual API calls)
- Isolates controller logic from external dependencies

### 8.3 Test Patterns

**Arrange-Act-Assert:**
```java
// Arrange
when(llmClient.generateStory(anyString())).thenReturn(mockStory);

// Act
mockMvc.perform(post("/api/stories")
        .content("{\"prompt\": \"test\"}"))

// Assert
.andExpect(status().isOk())
.andExpect(jsonPath("$.title").value("Expected Title"));
```

---

## 9. Build & Deployment

### 9.1 Build Process

```bash
mvn clean package -DskipTests
```

**What happens:**
1. `clean`: Deletes `target/` directory
2. `package`: Compiles Java → bytecode → JAR
3. `-DskipTests`: Skips test execution

**Output:** `target/storygen-0.0.1-SNAPSHOT.jar`

### 9.2 JAR File

**Executable JAR:**
- Contains all dependencies (fat JAR)
- Includes embedded Tomcat
- Can run with `java -jar`

**Contents:**
```
storygen-0.0.1-SNAPSHOT.jar
├── META-INF/
├── com/storygen/...          # Your compiled classes
├── org/springframework/...   # Spring Boot
├── org/apache/tomcat/...     # Embedded Tomcat
└── BOOT-INF/classes/
    ├── application.yml       # Configuration
    └── static/               # Frontend files
        ├── index.html
        ├── app.js
        └── style.css
```

### 9.3 Running

```bash
# Option 1: Run script
./run.sh

# Option 2: Direct
java -jar target/storygen-0.0.1-SNAPSHOT.jar
```

**run.sh:**
```bash
#!/bin/bash
pkill -f "storygen-0.0.1-SNAPSHOT.jar" 2>/dev/null  # Kill existing
sleep 1
java -jar target/storygen-0.0.1-SNAPSHOT.jar         # Start
pkill -f "storygen-0.0.1-SNAPSHOT.jar" 2>/dev/null  # Cleanup on exit
```

### 9.4 Server Startup Sequence

1. `java -jar` starts JVM
2. Spring Boot initializes
3. Component scanning finds all `@Component`, `@Service`, `@Controller`
4. Dependency injection wires everything together
5. `@PostConstruct` methods run (creates storage directory)
6. Embedded Tomcat starts on port 8080
7. Application ready to accept requests

---

## 10. Security Considerations

### 10.1 API Key Protection

- Never hardcoded in source code
- Stored in `.env` (gitignored)
- Read from environment variable at runtime

### 10.2 Input Validation

```java
@NotBlank(message = "prompt must not be blank")
@Size(max = 2000, message = "prompt must not exceed 2000 characters")
String prompt
```

### 10.3 Path Traversal Protection

```java
private static final Pattern VALID_ID = Pattern.compile("^story_[a-z0-9]{8}$");

private void validateId(String id) {
    if (!VALID_ID.matcher(id).matches()) {
        throw new IllegalArgumentException("Invalid story ID");
    }
}
```

Prevents: `../../etc/passwd` attacks

### 10.4 XSS Prevention

```java
private String escapeHtml(String text) {
    return text.replace("&", "&amp;")
               .replace("<", "&lt;")
               .replace(">", "&gt;");
}
```

HTML-escapes all user-generated content.

### 10.5 CORS (Not configured - single origin)

The frontend and backend run on the same origin (localhost:8080), so CORS isn't needed.

---

## 11. API Documentation

### POST /api/stories

**Request:**
```json
{
  "prompt": "a lighthouse keeper who talks to the sea"
}
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

**Errors:**
- `400 Bad Request`: Invalid or missing prompt
- `502 Bad Gateway`: LLM API error
- `500 Internal Server Error`: Unexpected error

### GET /api/stories/{id}/download

**Response:** HTML file download

**Errors:**
- `404 Not Found`: Story not found or invalid ID

---

## 12. Flow Diagrams

### Request Flow

```
User → Browser → HTTP POST → Spring Controller → LlmClient → NVIDIA NIM API
                                ↓
                         StoryToHtmlConverter
                                ↓
                         HtmlDocumentBuilder
                                ↓
                         FileStorageService → Disk
                                ↓
                         StoryResponse → Browser → User
```

### Dependency Injection Flow

```
Spring Container starts
    ↓
Component scanning finds:
  - StoryController
  - NvidiaNimLlmClient (via LlmClientConfig)
  - StoryToHtmlConverter
  - HtmlDocumentBuilder
  - FileStorageService
    ↓
Creates beans and injects dependencies:
  - StoryController ← LlmClient, Converter, Builder, Storage
    ↓
Application ready
```

---

## Summary

**This application demonstrates:**
- Clean layered architecture
- Interface-based design for flexibility
- Spring Boot's dependency injection and auto-configuration
- Deterministic text-to-HTML conversion
- Secure credential management
- Comprehensive error handling
- Unit and integration testing patterns
- Modern Java features (records, text blocks)
- RESTful API design
- Production-ready structure
