
# ArchPilot Backend — System Design, Diagram Generation & AI Review API

The **ArchPilot Backend** is a Java and Spring Boot REST API that powers the ArchPilot system design platform.

It allows developers to create and manage software architecture projects, define architectural components and their connections, automatically generate Mermaid.js diagrams, and evaluate system designs using rule-based checks and AI-powered architecture reviews.

Instead of storing static diagram images or raw text, the backend stores structured architecture data in a relational database. This allows diagrams and architecture reviews to be regenerated whenever a user modifies a system design.

The backend is responsible for:

- Managing users and system architecture projects.
- Creating and maintaining architectural components and their relationships.
- Generating Mermaid.js diagram definitions.
- Performing deterministic and AI-powered architecture reviews.
- Enforcing authentication, authorization, and project ownership.
- Validating requests and handling application errors.
- Securely managing database connections and AI provider integration.

---

## Table of Contents

- [Backend Core Responsibilities](#backend-core-responsibilities)
- [Backend Project Structure](#backend-project-structure)
- [Project Architecture and File Responsibilities](#project-architecture-and-file-responsibilities)
- [Tech Stack](#tech-stack)
- [Data Model Overview](#data-model-overview)
- [REST API Reference](#rest-api-reference)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation and Setup](#installation-and-setup)
  - [Database Setup](#database-setup)
  - [Environment Variables](#environment-variables)
  - [Running the Application](#running-the-application)
- [Available Maven Commands](#available-maven-commands)
- [Contributing](#contributing-fork-and-pull-request)
- [Development Guidelines](#development-guidelines)
- [Disclaimer](#disclaimer)

---

## Backend Core Responsibilities

### 1. System Design Workspace & Access Control

- **Project Management:** Create, retrieve, update, and delete system architecture projects.

- **Component Management:** Add, update, and remove architectural components such as Frontend, Backend, Database, Cache, and External API.

- **Connection Management:** Create directional connections between architectural components.

- **Graph Validation:** Ensure that connected components belong to the same project, prevent self-loops, and reject invalid component references.

- **Authentication:** Authenticate users using JWT-based authentication.

- **Ownership Authorization:** Ensure users can only access and manage projects they own.

### 2. Automatic Mermaid Diagram Generation

- **Database-Driven Generation:** Retrieve stored components and connections to generate architecture diagrams.

- **Syntax Translation:** Convert architectural components and relationships into valid Mermaid.js syntax.

- **Dynamic Updates:** Generate updated diagrams whenever the architecture changes.

- **Diagram API:** Expose generated diagram definitions through:

  `GET /api/projects/:id/diagram`

### 3. AI-Powered Architecture Review

- **Deterministic Checks:** Identify common architecture problems using predefined rules.

- **AI Integration:** Send sanitized architecture information to an external AI provider for evaluation.

- **Structured Findings:** Return findings containing the affected component, severity, issue, explanation, and recommended fix.

- **Schema Validation:** Validate AI responses before saving them in the database.

- **Review History:** Store architecture review findings with timestamps.

- **Security:** Protect AI API keys and apply rate limiting to review requests.

- **Read-Only Analysis:** AI reviews must never automatically modify project architecture data.

---

## Backend Project Structure

The project follows a layered Spring Boot architecture that separates API controllers, business logic, data persistence, security, and configuration.

Each contributor should create files inside the appropriate package according to their responsibilities.

```text id="archtree"
archpilot-backend/
│
├── src/
│   ├── main/
│   │   ├── java/com/archpilot/
│   │   │   │
│   │   │   ├── config/
│   │   │   │   # Application configuration for Spring Security,
│   │   │   │   # CORS, rate limiting, and external API clients.
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   # Handles incoming HTTP requests and responses.
│   │   │   │   │
│   │   │   │   ├── AuthController.java
│   │   │   │   │   # Authentication endpoints for user login
│   │   │   │   │   # and registration.
│   │   │   │   │
│   │   │   │   ├── ProjectController.java
│   │   │   │   │   # REST endpoints for creating, listing,
│   │   │   │   │   # updating, and deleting projects.
│   │   │   │   │
│   │   │   │   ├── ComponentController.java
│   │   │   │   │   # Handles component creation, updates,
│   │   │   │   │   # deletion, and project associations.
│   │   │   │   │
│   │   │   │   ├── ConnectionController.java
│   │   │   │   │   # Handles connections between components.
│   │   │   │   │
│   │   │   │   ├── DiagramController.java
│   │   │   │   │   # Exposes Mermaid diagram generation APIs.
│   │   │   │   │
│   │   │   │   └── ReviewController.java
│   │   │   │       # Triggers architecture reviews and
│   │   │   │       # retrieves previous review findings.
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   # Defines request and response data structures.
│   │   │   │   │
│   │   │   │   ├── ProjectDto.java
│   │   │   │   │   # Transfers project details between API
│   │   │   │   │   # clients and application services.
│   │   │   │   │
│   │   │   │   ├── ComponentDto.java
│   │   │   │   │   # Defines component input and output data.
│   │   │   │   │
│   │   │   │   ├── ConnectionDto.java
│   │   │   │   │   # Defines source, target, and connection data.
│   │   │   │   │
│   │   │   │   ├── DiagramResponse.java
│   │   │   │   │   # Returns generated Mermaid syntax.
│   │   │   │   │
│   │   │   │   └── ReviewFindingDto.java
│   │   │   │       # Represents structured architecture
│   │   │   │       # review findings returned to clients.
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   # Centralizes application exception handling.
│   │   │   │   │
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │       # Converts exceptions into consistent
│   │   │   │       # HTTP error responses.
│   │   │   │
│   │   │   ├── model/
│   │   │   │   # JPA entities representing database tables.
│   │   │   │   │
│   │   │   │   ├── User.java
│   │   │   │   │   # Stores user identity and credentials.
│   │   │   │   │
│   │   │   │   ├── Project.java
│   │   │   │   │   # Represents a system architecture project.
│   │   │   │   │
│   │   │   │   ├── Component.java
│   │   │   │   │   # Represents an architecture component.
│   │   │   │   │
│   │   │   │   ├── Connection.java
│   │   │   │   │   # Represents a directional relationship
│   │   │   │   │   # between architecture components.
│   │   │   │   │
│   │   │   │   └── ReviewFinding.java
│   │   │   │       # Stores identified architecture issues
│   │   │   │       # and recommended improvements.
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   # Provides database access using Spring Data JPA.
│   │   │   │   │
│   │   │   │   ├── UserRepository.java
│   │   │   │   │   # Database operations for users.
│   │   │   │   │
│   │   │   │   ├── ProjectRepository.java
│   │   │   │   │   # Stores and retrieves projects,
│   │   │   │   │   # including ownership-based queries.
│   │   │   │   │
│   │   │   │   ├── ComponentRepository.java
│   │   │   │   │   # Queries components belonging to projects.
│   │   │   │   │
│   │   │   │   ├── ConnectionRepository.java
│   │   │   │   │   # Stores and retrieves component connections.
│   │   │   │   │
│   │   │   │   └── ReviewFindingRepository.java
│   │   │   │       # Persists and retrieves review history.
│   │   │   │
│   │   │   ├── security/
│   │   │   │   # Implements authentication and authorization.
│   │   │   │   │
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   # Validates JWT tokens for protected requests.
│   │   │   │   │
│   │   │   │   └── OwnershipValidator.java
│   │   │   │       # Verifies that the authenticated user
│   │   │   │       # owns the requested project.
│   │   │   │
│   │   │   ├── service/
│   │   │   │   # Contains application business logic.
│   │   │   │   │
│   │   │   │   ├── ProjectService.java
│   │   │   │   │   # Handles project operations and ownership.
│   │   │   │   │
│   │   │   │   ├── ComponentService.java
│   │   │   │   │   # Manages components and validates their
│   │   │   │   │   # project associations.
│   │   │   │   │
│   │   │   │   ├── ConnectionService.java
│   │   │   │   │   # Validates and manages component connections.
│   │   │   │   │
│   │   │   │   ├── MermaidGeneratorService.java
│   │   │   │   │   # Converts components and connections into
│   │   │   │   │   # valid Mermaid.js diagram syntax.
│   │   │   │   │
│   │   │   │   ├── DeterministicCheckService.java
│   │   │   │   │   # Runs rule-based architecture checks.
│   │   │   │   │
│   │   │   │   └── AiReviewService.java
│   │   │   │       # Integrates with the AI provider, validates
│   │   │   │       # responses, and stores review findings.
│   │   │   │
│   │   │   └── ArchPilotApplication.java
│   │   │       # Main Spring Boot application entry point.
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       │   # Default Spring Boot application configuration.
│   │       │
│   │       └── application-dev.properties
│   │           # Development-specific configuration.
│   │
│   └── test/
│       └── java/com/archpilot/
│           # Unit and integration tests for controllers,
│           # services, repositories, validation, and security.
│
├── .env.example
│   # Template listing required environment variables
│   # without containing real credentials.
│
├── .env
│   # Local environment variables with actual values.
│   # Must be created by contributors and never committed.
│
├── .gitignore
│   # Excludes local secrets, build artifacts,
│   # and IDE-generated files from Git.
│
├── mvnw
│   # Maven wrapper executable for Linux and macOS.
│
├── mvnw.cmd
│   # Maven wrapper executable for Windows.
│
├── pom.xml
│   # Defines Maven dependencies, plugins, and build settings.
│
└── README.md
    # Project documentation and contribution instructions.
```

> **Important:** The structure above is the intended organization for the project. The `.env` file is created locally by each contributor and must not be committed to GitHub.

---

## Project Architecture and File Responsibilities

### 1. Controller Layer

**Package:** `com.archpilot.controller`

Controllers receive HTTP requests from the frontend and return API responses.

| File | Responsibility |
|------|----------------|
| `AuthController.java` | Handles user registration and login requests |
| `ProjectController.java` | Exposes endpoints for managing projects |
| `ComponentController.java` | Handles component CRUD requests |
| `ConnectionController.java` | Handles creation and deletion of connections |
| `DiagramController.java` | Returns generated Mermaid diagrams |
| `ReviewController.java` | Triggers and retrieves architecture reviews |

**Implementation Guidelines:**

- Controllers should not contain complex business logic.
- Use service classes for application operations.
- Accept and return DTOs instead of exposing JPA entities directly.
- Apply request validation.
- Return appropriate HTTP status codes.

### 2. Service Layer

**Package:** `com.archpilot.service`

Services implement the business rules of the application.

| File | Responsibility |
|------|----------------|
| `ProjectService.java` | Creates, updates, retrieves, and deletes projects |
| `ComponentService.java` | Manages architectural components and validates project relationships |
| `ConnectionService.java` | Validates source and target components before saving connections |
| `MermaidGeneratorService.java` | Converts architecture entities into Mermaid.js syntax |
| `DeterministicCheckService.java` | Detects common architecture problems using predefined rules |
| `AiReviewService.java` | Communicates with AI providers, validates findings, and stores review results |

**Implementation Guidelines:**

- Keep business logic inside service classes.
- Use constructor dependency injection.
- Use transactions where multiple database operations must succeed together.
- Validate project ownership before accessing protected architecture data.
- Avoid placing database queries directly in controllers.

### 3. Repository Layer

**Package:** `com.archpilot.repository`

Repositories communicate with the database through Spring Data JPA.

| File | Responsibility |
|------|----------------|
| `UserRepository.java` | Find users by ID or email |
| `ProjectRepository.java` | Retrieve projects and filter them by owner |
| `ComponentRepository.java` | Retrieve components belonging to a project |
| `ConnectionRepository.java` | Manage stored component relationships |
| `ReviewFindingRepository.java` | Store and retrieve architecture review findings |

Repositories should be injected into service classes rather than directly into controllers.

### 4. Model Layer

**Package:** `com.archpilot.model`

Models represent the application's database entities.

| File | Responsibility |
|------|----------------|
| `User.java` | Represents registered users |
| `Project.java` | Represents a system architecture project |
| `Component.java` | Represents frontend, backend, database, and other architecture components |
| `Connection.java` | Represents directional relationships between components |
| `ReviewFinding.java` | Stores architecture review findings |

Use JPA annotations to define entity relationships and database mappings.

### 5. DTO Layer

**Package:** `com.archpilot.dto`

DTOs define the data exchanged between the frontend and backend.

- `ProjectDto.java` — Project information and required input fields.
- `ComponentDto.java` — Component details and properties.
- `ConnectionDto.java` — Source and target component identifiers.
- `DiagramResponse.java` — Generated diagram syntax and project identifier.
- `ReviewFindingDto.java` — Architecture review details and recommendations.

Use appropriate validation annotations for incoming request data.

### 6. Security Layer

**Package:** `com.archpilot.security`

- `JwtAuthenticationFilter.java` — Extracts and validates JWT tokens from incoming requests and establishes the authenticated user.
- `OwnershipValidator.java` — Checks whether the authenticated user owns a requested project.

Security configuration should protect application endpoints and restrict access based on authentication and ownership.

### 7. Configuration Layer

**Package:** `com.archpilot.config`

This package contains Spring configuration classes for:

- Spring Security and authentication.
- Cross-Origin Resource Sharing (CORS).
- AI provider HTTP clients.
- Rate limiting.
- Other shared application configurations.

Configuration classes may be added as required by the implementation.

### 8. Exception Handling

**Package:** `com.archpilot.exception`

`GlobalExceptionHandler.java` handles exceptions raised by controllers and services.

It should return consistent error responses for:

- Invalid request data (`400 Bad Request`).
- Authentication failures (`401 Unauthorized`).
- Unauthorized project access (`403 Forbidden`).
- Missing resources (`404 Not Found`).
- Business rule conflicts (`409 Conflict`).
- Unexpected server errors (`500 Internal Server Error`).

### 9. Testing

**Package:** `src/test/java/com/archpilot/`

Contributors should create tests for the functionality they implement.

Tests should cover:

- Project CRUD operations.
- Authentication and authorization.
- Component and connection validation.
- Mermaid diagram generation.
- Deterministic architecture checks.
- AI response validation.
- Exception handling.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17+ |
| Framework | Spring Boot 3.x |
| REST API | Spring Web |
| Persistence | Spring Data JPA |
| Security | Spring Security |
| Validation | Jakarta Bean Validation |
| Database | PostgreSQL / MySQL |
| Local Testing Database | H2 |
| Authentication | JWT |
| AI Integration | Spring RestClient / WebClient |
| JSON Processing | Jackson |
| Build Tool | Maven |
| Testing | JUnit / Spring Boot Test |

---

## Data Model Overview

### User

| Field | Description |
|-------|-------------|
| `id` | Unique user identifier |
| `email` | User email address |
| `passwordHash` | Securely hashed password |
| `createdAt` | Account creation timestamp |

### Project

| Field | Description |
|-------|-------------|
| `id` | Unique project identifier |
| `ownerId` | Reference to the project owner |
| `name` | Project name |
| `description` | Project description |
| `requirements` | System design requirements |
| `createdAt` | Project creation timestamp |
| `updatedAt` | Last modification timestamp |

### Component

| Field | Description |
|-------|-------------|
| `id` | Unique component identifier |
| `projectId` | Associated project |
| `name` | Component name |
| `type` | FRONTEND, BACKEND, DATABASE, CACHE, EXTERNAL_API, etc. |
| `description` | Component description |

### Connection

| Field | Description |
|-------|-------------|
| `id` | Unique connection identifier |
| `projectId` | Associated project |
| `sourceComponentId` | Source component |
| `targetComponentId` | Target component |
| `label / protocol` | Connection label or communication protocol |

### ReviewFinding

| Field | Description |
|-------|-------------|
| `id` | Unique finding identifier |
| `projectId` | Associated project |
| `componentName` | Affected architecture component |
| `severity` | HIGH, MEDIUM, LOW |
| `issue` | Identified architecture problem |
| `explanation` | Explanation of the issue |
| `recommendation` | Suggested improvement |
| `reviewedAt` | Review timestamp |

---

## REST API Reference

The backend exposes REST endpoints consumed by the ArchPilot Frontend.

### 1. Projects

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects` | Create a system design project |
| GET | `/api/projects` | Retrieve projects belonging to the authenticated user |
| GET | `/api/projects/:id` | Retrieve a project with its components and connections |
| PUT | `/api/projects/:id` | Update project information |
| DELETE | `/api/projects/:id` | Delete a project and its associated architecture data |

### 2. Components & Connections

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects/:id/components` | Add an architectural component |
| PUT | `/api/components/:id` | Update an existing component |
| DELETE | `/api/components/:id` | Delete a component and clean up linked connections |
| POST | `/api/projects/:id/connections` | Create a validated connection |
| DELETE | `/api/connections/:id` | Remove a connection |

### 3. Diagrams & AI Reviews

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/projects/:id/diagram` | Generate and return Mermaid diagram syntax |
| POST | `/api/projects/:id/reviews` | Run deterministic checks and AI review |
| GET | `/api/projects/:id/reviews` | Retrieve previous architecture review findings |

### Example Diagram Response

**Endpoint:**

`GET /api/projects/1/diagram`

**Response:**

```json id="archdiagjson"
{
  "projectId": 1,
  "mermaidSyntax": "graph TD\n  C1[Web Frontend] --> C2[Spring Boot API]\n  C2 --> C3[(PostgreSQL DB)]"
}
```

### Example Architecture Review Finding

```json id="archreviewjson"
{
  "component": "Database",
  "severity": "Medium",
  "issue": "Possible performance bottleneck",
  "explanation": "High traffic may overload the database.",
  "recommendation": "Consider indexing and caching."
}
```

---

## Getting Started

Follow these instructions to configure and run the backend locally.

### Prerequisites

Ensure you have the following installed:

- **Java Development Kit:** JDK 17 or higher
- **Maven:** Version 3.8+ or use the included Maven wrapper
- **Database:** PostgreSQL or MySQL
- **Git:** For cloning and managing the repository
- **AI Provider API Key:** Required for AI-powered architecture reviews

Verify Java installation:

```bash id="archjava"
java -version
```

Verify Git installation:

```bash id="archgit"
git --version
```

### Installation and Setup

#### 1. Fork the Repository

Before contributing, fork the original repository into your GitHub account.

#### 2. Clone Your Fork

```bash id="archclone"
git clone https://github.com/<your-username>/<backend-repo-name>.git
cd <backend-repo-name>
```

#### 3. Install Dependencies

For Linux and macOS:

```bash id="archinstall"
./mvnw clean install
```

For Windows:

```powershell id="archwin"
.\mvnw.cmd clean install
```

Maven will download the required project dependencies.

### Database Setup

Create a local database before running the application.

**Example using PostgreSQL:**

```sql id="archsql"
CREATE DATABASE archpilot;
```

Ensure PostgreSQL is running and that your configured database user has access to this database.

---

## Environment Variables

### Important: Required for All Contributors

Every contributor must create their own `.env` file using the `.env.example` file provided in the repository.

The `.env.example` file contains the names of the required configuration variables and placeholder values.

The `.env` file must contain actual values for the contributor's local development environment.

### 1. Create the .env File

After cloning the repository, locate:

```text id="archenvtree"
archpilot-backend/
├── .env.example
└── ...
```

Create a copy named `.env`.

**Linux / macOS:**

```bash id="archenvcopy"
cp .env.example .env
```

**Windows PowerShell:**

```powershell id="archenvwin"
Copy-Item .env.example .env
```

### 2. Configure Environment Variables

Open the newly created `.env` file and replace all placeholder values with your actual local configuration.

**Example `.env.example`:**

```dotenv id="archenvexample"
DB_URL=jdbc:postgresql://localhost:5432/archpilot
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password

JWT_SECRET=your_256_bit_secret_signing_key

AI_API_URL=https://api.openai.com/v1/chat/completions
AI_API_KEY=your_ai_provider_api_key

CORS_ALLOWED_ORIGINS=http://localhost:3000
```

**Example local `.env` configuration:**

```dotenv id="archenvlocal"
DB_URL=jdbc:postgresql://localhost:5432/archpilot
DB_USERNAME=postgres
DB_PASSWORD=replace_with_your_actual_database_password

JWT_SECRET=replace_with_a_secure_random_signing_key

AI_API_URL=https://api.openai.com/v1/chat/completions
AI_API_KEY=replace_with_your_actual_ai_api_key

CORS_ALLOWED_ORIGINS=http://localhost:3000
```

> **Important:** The values above are examples. Every contributor must use their actual database credentials, generated JWT secret, AI API key, and frontend origin.

### 3. Configure Spring Boot to Read Environment Variables

Spring Boot can resolve environment variables through `application.properties`.

Example configuration:

**File:** `src/main/resources/application.properties`

```properties id="archprops"
spring.application.name=archpilot-backend

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

jwt.secret=${JWT_SECRET}

ai.api.url=${AI_API_URL}
ai.api.key=${AI_API_KEY}

app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS}

server.port=8080
```

> **Note:** Spring Boot does not automatically load a `.env` file by default. Contributors must use the project's configured `.env` loading mechanism or export the variables into their environment before running the application.

For example, on Linux or macOS, if using a standard shell-compatible `.env` file:

```bash id="archexport"
set -a
source .env
set +a
./mvnw spring-boot:run
```

For Windows, configure the variables in your IDE run configuration or use the environment-loading mechanism configured by the project.

### 4. Never Commit the .env File

The `.env` file contains sensitive credentials and must never be pushed to GitHub.

Ensure `.gitignore` contains:

```gitignore id="archignore"
.env
.env.*
!.env.example

target/
.idea/
*.iml
```

**Contribution rules:**

1. Always copy `.env.example` to `.env`.
2. Replace the placeholders with your actual values.
3. Never commit passwords, API keys, or signing secrets.
4. Never modify `.env.example` to include real credentials.
5. If your feature introduces a new environment variable, update `.env.example` with a safe placeholder and document its purpose.
6. Keep local `.env` values private.

---

## Running the Application

### 1. Start the Database

Ensure your PostgreSQL or MySQL service is running.

### 2. Verify Environment Configuration

Confirm that the environment variables have been configured and made available to Spring Boot.

### 3. Start the Backend Server

**Linux / macOS:**

```bash id="archrun"
./mvnw spring-boot:run
```

**Windows:**

```powershell id="archrunwin"
.\mvnw.cmd spring-boot:run
```

The backend should start at:

`http://localhost:8080`

API endpoints will be accessible under:

`http://localhost:8080/api`

> **Note:** Protected endpoints require authentication. Starting the server successfully does not mean that protected endpoints can be accessed without a valid token.

---

## Available Maven Commands

| Command | Description |
|---------|-------------|
| `./mvnw spring-boot:run` | Starts the Spring Boot application |
| `./mvnw test` | Runs unit and integration tests |
| `./mvnw clean package` | Builds an executable JAR inside `target/` |
| `./mvnw clean install` | Builds, tests, and installs the artifact locally |
| `./mvnw clean verify` | Runs the Maven verification lifecycle |

For Windows, use `.\mvnw.cmd` instead of `./mvnw`.

---

## Contributing (Fork & Pull Request)

Contributions to the ArchPilot Backend are welcome.

All contributors must follow the project architecture, environment configuration, and Git workflow.

### 1. Fork the Repository

1. Open the original GitHub repository.
2. Click **Fork**.
3. Create a copy under your GitHub account.

### 2. Clone Your Fork

```bash id="archfork"
git clone https://github.com/<your-username>/<backend-repo-name>.git
cd <backend-repo-name>
```

### 3. Add the Upstream Remote

```bash id="archupstream"
git remote add upstream https://github.com/<original-owner>/<backend-repo-name>.git
```

Verify the configured remotes:

```bash id="archremotes"
git remote -v
```

### 4. Configure Your Local Environment

Before writing or testing code, contributors must:

1. Copy `.env.example` to `.env`.
2. Replace all placeholder values with actual local values.
3. Create and configure the local database.
4. Ensure the database service is running.
5. Confirm the required environment variables are available to Spring Boot.
6. Start the backend and verify the application runs.

**Linux / macOS:**

```bash id="archcontributorenv"
cp .env.example .env
```

**Windows PowerShell:**

```powershell id="archcontributorenvwin"
Copy-Item .env.example .env
```

The `.env` file must remain local and must not be included in commits or Pull Requests.

### 5. Create a Feature Branch

Never commit directly to `main`.

Create a separate branch for each feature or bug fix.

```bash id="archfeature"
git checkout -b feature/your-feature-name
```

Examples:

- `feature/project-management`
- `feature/component-validation`
- `feature/mermaid-generation`
- `feature/ai-review`
- `feature/jwt-authentication`
- `fix/project-ownership-check`

### 6. Implement Your Changes

Follow the established layered architecture:

```text id="archlayers"
HTTP Request
     |
     v
Controller
     |
     v
Service
     |
     v
Repository
     |
     v
Database
```

For example, when implementing project creation:

1. `ProjectController.java` receives the HTTP request.
2. `ProjectDto.java` defines and validates the request data.
3. `ProjectService.java` performs business validation and prepares the project.
4. `OwnershipValidator.java` checks ownership where required.
5. `ProjectRepository.java` saves the project.
6. `Project.java` represents the database entity.
7. The controller returns an appropriate HTTP response.

### 7. Test Your Changes

Before committing, contributors must:

- Verify that new endpoints work correctly.
- Confirm request validation is implemented.
- Check authentication and ownership authorization.
- Test expected error responses.
- Ensure existing functionality still works.
- Add relevant unit or integration tests.
- Confirm the project builds successfully.

Run:

```bash id="archtests"
./mvnw test
./mvnw clean verify
```

### 8. Commit Your Changes

Stage your changes:

```bash id="archadd"
git add .
```

Commit with a descriptive message:

```bash id="archcommit"
git commit -m "feat: add deterministic architecture validation"
```

Recommended commit prefixes:

| Prefix | Purpose |
|--------|---------|
| `feat:` | New functionality |
| `fix:` | Bug fix |
| `refactor:` | Code restructuring |
| `test:` | Adding or updating tests |
| `docs:` | Documentation changes |
| `chore:` | Maintenance and configuration |

### 9. Push Your Feature Branch

```bash id="archpush"
git push origin feature/your-feature-name
```

### 10. Open a Pull Request

1. Open your forked repository on GitHub.
2. Click **Compare & pull request**.
3. Target the corresponding  branch from the original repository.
4. Provide a clear description of your implementation.
5. Mention any newly added endpoints.
6. Document any new environment variables.
7. Include database migrations if applicable.
8. Include sample API requests and responses when relevant.
9. Confirm that all tests pass.

---

## Development Guidelines

### 1. Follow the Layered Architecture

Keep controllers, services, repositories, entities, and DTOs separated according to their responsibilities.

### 2. Use Constructor Dependency Injection

Prefer constructor injection when connecting services and repositories.

Avoid unnecessary field injection.

### 3. Validate All Requests

Use Jakarta Bean Validation for incoming request data.

Apply additional business validation inside the service layer.

### 4. Enforce Authentication and Ownership

All protected operations must verify that the current user has permission to access or modify the requested resource.

### 5. Keep API Responses Consistent

Use DTOs for API responses.

Handle exceptions through the global exception handler.

### 6. Protect Sensitive Configuration

Never expose:

- Database credentials
- JWT signing secrets
- AI provider API keys
- Other confidential environment variables

### 7. Keep AI Reviews Read-Only

AI-generated recommendations are advisory.

AI responses must not directly create, update, or delete architecture components.

### 8. Update Documentation

When introducing new functionality:

- Update the relevant API documentation.
- Document any new environment variables.
- Update `.env.example` when configuration requirements change.
- Add tests for important business logic.
- Keep the project structure documentation accurate.

---

## Disclaimer

> **Important:** AI-generated architecture reviews are intended to assist developers during early system planning and help identify common engineering trade-offs.
>
> These reviews do not constitute a formal security audit, penetration test, or compliance certification.
>
> Developers remain responsible for validating security, scalability, reliability, and production readiness before deploying their systems.
