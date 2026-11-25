
# ArchPilot — Project Creation

## 1. Objective

Implement the functionality that allows an authenticated user to create a new system architecture project.

The authenticated user who creates the project automatically becomes its OWNER and the invited users  must accept the invitation as the COLLABORATOR ,
or even continue to work on his self profile as the OWNER.

## 2. Existing Functionality

- Google OAuth authentication is already implemented.
- GitHub OAuth authentication is already implemented.
- Reuse the existing authentication and user management implementation.
- Do not rebuild or modify working authentication functionality ,only when necessary.
- The system supports only two project roles:
    - OWNER
    - COLLABORATOR

Roles must be project-specific rather than global user roles.

## 3. Functional Requirements

- Allow authenticated users to create projects.
- Automatically associate the project with the authenticated user.
- Automatically assign the OWNER role to the project creator.
- when someone invited to collaborate must firstly login then accept the invitation
- Validate project information before saving.
- Store project information in the database.
- Automatically generate project identifiers and timestamps.
- Return the created project information to the frontend.

## 4. Database Model

### 4.1 Project Entity

| Field | Type | Description |
|---|---|---|
| id | Existing ID type | Unique project identifier |
| name | String | Project name |
| description | String | Project description |
| requirements | String | System design requirements |
| owner | User | Authenticated user who created the project |
| createdAt | LocalDateTime or Instant | Project creation timestamp |
| updatedAt | LocalDateTime or Instant | Last modification timestamp |

### 4.2 Entity Relationships

- A user can own multiple projects.
- A project belongs to one owner.
- A project can have many collaborators
- each  user can be the owner or even collaborator but in different project
- Implement a Many-to-One relationship between Project and User.
- Preserve the existing user entity and identifier type.
- Avoid creating duplicate user records.

### 4.3 Project Ownership

- The owner must be retrieved from the existing authentication context.
- The project creator automatically becomes the OWNER.
- If a project membership entity already exists, create the owner membership within the same transaction.
- Otherwise, derive the OWNER role from the project's owner reference.

## 5. Project Structure

### 5.1 Project Entity

Responsible for:

- Defining the project database model.
- Establishing the relationship with the User entity.
- Managing creation and modification timestamps.
- Defining appropriate database constraints.

### 5.2 CreateProjectRequest DTO

Responsible for receiving and validating:

- name
- description
- requirements

Use Jakarta Bean Validation.

### 5.3 ProjectResponse DTO

Responsible for returning:

- Project ID
- Project name
- Project description
- Project requirements
- Owner ID
- Authenticated user's project role
- Creation timestamp
- Modification timestamp

Do not expose sensitive user information or return JPA entities directly.

### 5.4 ProjectRepository

Responsible for:

- Persisting project information.
- Communicating with the database.
- Extending the appropriate Spring Data JPA repository.

### 5.5 ProjectService

Responsible for:

- Retrieving the authenticated user.
- Validating project information.
- Creating the project.
- Assigning project ownership.
- Saving the project within a transaction.
- Mapping the saved entity to a response DTO.

Use constructor dependency injection.

### 5.6 ProjectController

Responsible for:

- Receiving project creation requests.
- Validating request DTOs.
- Calling ProjectService.
- Returning appropriate HTTP responses.

Do not implement business logic inside the controller.

### 5.7 collaborator Entity

Responsible for:

- Defining collaborators information from the authenticated principal.
- how they are going to be connected to a specific project and who invited them
- Managing creation and modification timestamps.
- Defining appropriate database constraints.


## 6. REST API

### 6.1 Create Project

Method: POST

Endpoint: /api/projects

Authentication: Required

### 6.2 Request Body

```json
{
  "name": "E-commerce Platform",
  "description": "System architecture for an online shopping platform",
  "requirements": "Authentication, payment processing and product management"
}
```

### 6.3 Successful Response

HTTP Status: 201 Created

```json
{
  "id": 1,
  "name": "E-commerce Platform",
  "description": "System architecture for an online shopping platform",
  "requirements": "Authentication, payment processing and product management",
  "ownerId": 12,
  "role": "OWNER",
  "createdAt": "2026-11-25T12:00:00Z",
  "updatedAt": "2026-11-25T12:00:00Z"
}
```

Adapt the response to the existing API response structure and identifier types.

## 7. Validation Rules

| Field | Validation |
|---|---|
| name | Required, not blank, maximum 150 characters |
| description | Optional, maximum 2,000 characters |
| requirements | Optional, maximum 10,000 characters |
| owner | Automatically retrieved from authentication |
| createdAt | Automatically generated |
| updatedAt | Automatically generated |

Additional rules:

- Trim unnecessary whitespace from the project name.
- Reject empty project names.
- Prevent unauthorized ownership assignment.
- Allow users to create multiple projects.
- Project names do not need to be globally unique.

## 8. Business Logic

The project creation process must follow these steps:

1. Receive the project creation request.
2. Validate the request body.
3. Retrieve the authenticated user from the security context.
4. Resolve the authenticated identity to the existing local User entity.
5. Create a new Project entity.
6. Associate the authenticated user with the project as its owner.
7. Assign or derive the OWNER role.
8. Generate the creation and modification timestamps.
9. Persist the project in the database.
10. Map the saved entity to ProjectResponse.
11. Return HTTP 201 Created.

The entire persistence operation must be transactional.

## 9. Exception Handling

Reuse the existing GlobalExceptionHandler.

| Status Code | Description |
|---|---|
| 201 Created | Project successfully created |
| 400 Bad Request | Invalid project information |
| 401 Unauthorized | User is not authenticated |
| 403 Forbidden | Existing account policy denies the operation |
| 500 Internal Server Error | Unexpected application error |

Return consistent error responses according to the existing application structure.

Do not expose stack traces, credentials or internal database information.

## 10. Security Requirements

- Reuse existing Google and GitHub authentication.
- Protect the project creation endpoint.
- Never trust an owner ID provided by the frontend.
- Never trust a project role provided by the frontend.
- Retrieve the user identity from the existing authentication context.
- Maintain only OWNER and COLLABORATOR roles.
- Do not introduce additional authentication mechanisms.
- Do not expose OAuth tokens or sensitive user information.

## 11. Testing Requirements

### 11.1 Unit Testing

Create or update ProjectServiceTest.

Test the following:

- Successful project creation.
- Correct assignment of project ownership.
- Invalid project information.
- Missing authenticated user.
- Database persistence failure.
- Correct response DTO mapping.

### 11.2 Integration Testing

Create or update ProjectControllerTest.

Test the following:

- Authenticated Google users can create projects.
- Authenticated GitHub users can create projects.
- Unauthenticated users cannot create projects.
- Invalid requests return HTTP 400.
- Successful requests return HTTP 201.
- Users cannot manually assign project ownership.
- Project information is correctly persisted.
- Transaction failures roll back project creation.

Use existing authentication test helpers or mocked principals instead of real OAuth credentials.

## 12. Acceptance Criteria

- [ ] Authenticated users can successfully create projects.
- [ ] The project creator automatically becomes the OWNER.
- [ ] Project information is stored in the database.
- [ ] Input validation is implemented.
- [ ] DTOs are used for requests and responses.
- [ ] Unauthorized requests are rejected.
- [ ] Creation and modification timestamps are generated.
- [ ] Existing Google and GitHub authentication remains functional.
- [ ] A collaborator can create and own a different project.
- [ ] Unit and integration tests pass.
- [ ] The application builds successfully.

## 13. Implementation Constraints

- Follow the existing project architecture.
- Inspect README.md and CLAUDE.md before implementation.
- Inspect existing entities, repositories, services and controllers.
- Reuse existing implementations where possible.
- Do not create duplicate classes.
- Use the existing database migration mechanism.
- Do not drop or recreate existing database tables.
- Use constructor dependency injection.
- Use transactions where necessary.
- Maintain clean and readable code.
- Do not introduce unnecessary dependencies.
- Never commit environment variables or credentials.

## 14. Out of Scope

Do not implement the following functionalities during this task:

- Project listing.
- Project updates.
- Project deletion.
- Collaborator invitations.
- Collaborator management.
- Component management.
- Connection management.
- Mermaid diagram generation.
- AI architecture reviews.
- Authentication implementation.

These functionalities will be implemented in separate tasks.

## 15. Instructions for Claude Code

1. Inspect the existing repository.
2. Identify completed and missing project creation functionality.
3. Implement only the missing functionality.
4. Preserve existing authentication and application configurations.
5. Implement the necessary database migrations.
6. Implement validation and exception handling.
7. Create unit and integration tests.
8. Run the Maven build and tests.
9. Report modified files, implementation decisions and test results.
10. Report any errors or blockers.

### Verification Commands

Linux/macOS:

```bash
./mvnw test
./mvnw clean verify
```

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

Do not commit, push or deploy changes automatically.

## 16. Definition of Done

The task is complete when an authenticated user can create a project, automatically become its OWNER, retrieve the created project information from the API response, and all relevant tests pass without breaking existing functionality.

## 17. Proposed Defaults

Field length limits, optional fields and the exact API response structure are proposed defaults because the README does not explicitly define them.

Preserve compatible existing repository conventions and report any necessary changes.
