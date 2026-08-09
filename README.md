<img src='./tower-web/src/assets/landing/assets/img/nf-tower-purple.svg' width='500' alt='Nextflow Tower logo'/>

[![Chat on Gitter](https://img.shields.io/gitter/room/nf-tower/community.svg?colorB=26af64&style=popout)](https://gitter.im/nf-tower/community)

Nextflow Tower is an open source monitoring and management platform
for [Nextflow](https://www.nextflow.io/) workflows developed by [Seqera Labs](https://seqera.io).

## Availability
A free-to-use public Tower service is available at [tower.nf](https://tower.nf/).

**SIMPLIFIED SINGLE-USER VERSION**

This community version has been simplified for single-user operation with all authentication
removed. No login, registration, or email configuration required. Simply start the application
and begin monitoring your workflows immediately as a single default user.

The community version of Tower is available from this repository. It can be deployed in a
user's own environment and has features for single users to monitor their Nextflow
pipelines, deployed anywhere.

The fully-featured enterprise version of Tower is available from Seqera Labs. It can be
deployed in any on-premise or cloud environment and includes advanced workflow
management, resource optimization, and enterprise-grade support. To learn more, please
visit [Seqera Labs](https://seqera.io).

## Requirements 

* Java 21 (or Java 11/17)
* Gradle 8.7+ (wrapper included)
* Bun or Node.js (v22+)
* Docker / Podman (optional, for containerized run)

## Quick Start (One-Command Dev Launcher)

```bash
./start.sh
```
Or:
```bash
make dev
```

## Docker / Container Run

```bash
docker compose up
```


## Backend settings

Tower backend settings can be provided in either:

- `application.yml` in the backend class-path
- `tower.yml` in the launching directory

**SINGLE-USER MODE:** No SMTP or authentication configuration required. The application
automatically creates a default user (`user@tower.local`) on startup.

## Basic use case

Navigate to `http://localhost:8000` in your browser. You will be automatically logged in
as the default user and can immediately start monitoring your Nextflow workflows.

# Development

### Backend execution

**SINGLE-USER MODE:** No environment variables are required for authentication.

See `tower-backend/src/main/resources/application.yml` for further config details.

Launch the backend with the command:

```bash
./gradlew tower-backend:run --continuous
```

### Frontend execution

```bash
cd tower-web
npm install
npm run livedev
```

## Database

Tower is designed to be database agnostic and can use most popular SQL
database servers, such as MySql, Postgres, Oracle and many other.

By default it uses [H2](https://www.h2database.com), an embedded database meant to be used for evaluation purpose only.


## Environment variables:

* `TOWER_APP_NAME`: Application name (default: "Nextflow Tower").
* `TOWER_SERVER_URL`: Server URL e.g. `http://localhost:8000`.
* `TOWER_CONTACT_EMAIL`: Contact email e.g. `hello@foo.com`.
* `TOWER_DB_CREATE`: DB creation policy (default: `update`).
* `TOWER_DB_URL`: Database JDBC connection URL (default: `jdbc:h2:file:./.db/h2/tower`).
* `TOWER_DB_DRIVER`: Database JDBC driver class name (default: `org.h2.Driver`).
* `TOWER_DB_DIALECT`: Database SQL Hibernate dialect (default: `org.hibernate.dialect.H2Dialect`).
* `TOWER_DB_USER`: Database user name (default: `sa`).
* `TOWER_DB_PASSWORD`: Database user password (default: empty).

**Note:** SMTP settings are no longer used in single-user mode. Authentication has been removed.

# Backend REST API Documentation

## Executive Summary

Nextflow Tower is a sophisticated workflow monitoring and management platform built with **Micronaut framework** (not Spring Boot). The backend exposes a feature-rich REST API with **46+ endpoints** organized across **8 controllers**, supporting real-time workflow tracking, user management, authentication, and comprehensive task monitoring.

**Tech Stack:**
- **Framework:** Micronaut 1.3.3 (compile-time DI, AOT-ready)
- **Language:** Groovy 2.5.8
- **Server:** Netty (asynchronous, non-blocking)
- **ORM:** Hibernate GORM
- **Security:** Micronaut Security + JWT
- **API Docs:** OpenAPI 3.0 / Swagger

---

## API Architecture Overview

### Three-Tier Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENT LAYER                         │
│  (Web Frontend, CLI, Nextflow Plugin, API Consumers)   │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP/JSON
                      ▼
┌─────────────────────────────────────────────────────────┐
│              PRESENTATION LAYER                         │
│         Controllers (8 classes)                         │
│  • ServiceInfoController  • GateController              │
│  • TokenController        • UserController              │
│  • WorkflowController     • TraceController             │
│  • LiveEventsController   • BaseController              │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│               BUSINESS LOGIC LAYER                      │
│             Services (26+ classes)                      │
│  • WorkflowService      • TraceService                  │
│  • UserService          • TaskService                   │
│  • AccessTokenService   • GateService                   │
│  • ProgressService      • LiveEventsService             │
│  • AuditEventPublisher  • MailService                   │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│                 DATA ACCESS LAYER                       │
│         Domain Entities (21 classes)                    │
│  • User            • Workflow        • Task             │
│  • AccessToken     • WorkflowComment • TaskData         │
│  • WorkflowMetrics • Role            • UserRole         │
└─────────────────────┬───────────────────────────────────┘
                      │ GORM/Hibernate
                      ▼
┌─────────────────────────────────────────────────────────┐
│                  DATABASE LAYER                         │
│         MySQL 8.0 / H2 (embedded)                       │
└─────────────────────────────────────────────────────────┘
```

### Request/Response Flow

```
1. Client sends HTTP request with JWT/API token
2. Micronaut Security intercepts → validates credentials
3. Controller receives request → validates input
4. Service layer processes business logic
5. Domain entities persist/retrieve data via GORM
6. Response DTOs built and serialized to JSON
7. HTTP response sent back to client
```

---

## Complete API Endpoint Catalog

### Service Info Endpoints (`/`)
**Controller:** `ServiceInfoController.groovy`
**Security:** `IS_ANONYMOUS` (public access)

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/service-info` | Service metadata (version, API info) | ServiceInfoResponse |
| GET | `/ping` | Health check | 200 OK |

---

### Authentication & Registration (`/gate`)
**Controller:** `GateController.groovy`
**Security:** `IS_ANONYMOUS` (public)

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| POST | `/gate/access` | User registration with CAPTCHA | AccessGateRequest | AccessGateResponse |

**Key Features:**
- Google reCAPTCHA integration
- Email-based authentication
- Automatic user account creation
- JWT token generation

---

### Access Token Management (`/token`)
**Controller:** `TokenController.groovy`
**Security:** `ROLE_USER`

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/token/list` | List all user's tokens | ListAccessTokensResponse |
| POST | `/token/create` | Create new API token | CreateAccessTokenResponse |
| DELETE | `/token/delete/{tokenId}` | Delete specific token | MessageResponse |
| DELETE | `/token/delete-all` | Delete all tokens | MessageResponse |
| GET | `/token/default` | Get/create default token | GetDefaultTokenResponse |

**Implementation Details:**
- Tokens stored in `AccessToken` domain
- Last-used timestamp tracked asynchronously
- Default token auto-created per user
- Token format: 40-character hash

---

### User Management (`/user`)
**Controller:** `UserController.groovy`
**Security:** `IS_AUTHENTICATED` / `ADMIN`

| Method | Endpoint | Security | Description | Response |
|--------|----------|----------|-------------|----------|
| GET | `/user/` | IS_AUTHENTICATED | Get current user profile | DescribeUserResponse |
| POST | `/user/update` | IS_AUTHENTICATED | Update user profile | "User successfully updated!" |
| DELETE | `/user/delete` | IS_AUTHENTICATED | Delete own account | "User successfully deleted!" |
| DELETE | `/user/delete/{userId}` | ADMIN | Delete specific user | DeleteUserResponse |
| GET | `/user/list{?max,offset}` | IS_AUTHENTICATED | List users (paginated) | ListUserResponse |
| GET | `/user/get/{userId}` | ADMIN | Get user by ID | DescribeUserResponse |
| GET | `/user/allow/login/{userId}` | ADMIN | Enable user login | EnableUserResponse |

**User Model Fields:**
```groovy
- id (Long) - Primary key
- userName (String, unique, regex validated)
- email (String, unique)
- firstName, lastName, organization, description
- avatar (URL)
- trusted (boolean) - Admin flag
- disabled (Boolean) - Account status
- authToken (String, 40 chars) - Email auth token
- options (UserOptions) - User preferences
- dateCreated, lastUpdated, lastAccess
```

---

### Workflow Management (`/workflow`)
**Controller:** `WorkflowController.groovy`
**Security:** `ROLE_USER`
**Configuration:**
- Max workflows per request: 100 (configurable: `tower.workflow.list.max-allowed`)
- Max tasks per request: 100 (configurable: `tower.workflow.tasks.max-allowed`)

| Method | Endpoint | Description | Query Params | Response |
|--------|----------|-------------|--------------|----------|
| GET | `/workflow/list` | List user's workflows | `max`, `offset`, `search` | ListWorkflowResponse |
| GET | `/workflow/{workflowId}` | Get workflow details | - | GetWorkflowResponse |
| GET | `/workflow/{workflowId}/progress` | Get execution progress | - | GetProgressResponse |
| GET | `/workflow/{workflowId}/tasks` | List tasks (paginated) | `length`, `start`, `order[0][column]`, `order[0][dir]`, `search` | TaskList |
| GET | `/workflow/{workflowId}/task/{taskId}` | Get specific task | - | TaskGet |
| GET | `/workflow/{workflowId}/metrics` | Get workflow metrics | - | GetWorkflowMetricsResponse |
| GET | `/workflow/{workflowId}/comments` | List comments | - | ListWorkflowCommentsResponse |
| POST | `/workflow/{workflowId}/comment/add` | Add comment | AddWorkflowCommentRequest | AddWorkflowCommentResponse |
| PUT | `/workflow/{workflowId}/comment` | Update comment | UpdateWorkflowCommentRequest | UpdateWorkflowCommentResponse |
| DELETE | `/workflow/{workflowId}/comment` | Delete comment | DeleteWorkflowCommentRequest | DeleteWorkflowCommentResponse |
| DELETE | `/workflow/{workflowId}` | Mark workflow for deletion | - | HTTP 204 / MessageResponse |

**Workflow Model:**
```groovy
- id (String, 16 chars) - Workflow ID
- owner (User) - Foreign key
- submit, start, complete (OffsetDateTime)
- status (WorkflowStatus: RUNNING/SUCCEEDED/FAILED)
- sessionId, projectDir, profile
- commandLine (8096 chars max)
- repository, commitId, revision
- container, containerEngine
- exitStatus, errorMessage, errorReport
- duration (Long, milliseconds)
- params (JSON text) - Workflow parameters
- configFiles (JSON text)
- manifest (WfManifest) - Embedded
- nextflow (WfNextflow) - Embedded
- stats (WfStats) - Embedded
- deleted (Boolean) - Soft delete flag
```

---

### Workflow Execution Tracing (`/trace`)
**Controller:** `TraceController.groovy`
**Security:** `ROLE_USER`

#### Legacy API (Deprecated)
| Method | Endpoint | Description | Deprecated |
|--------|----------|-------------|------------|
| POST | `/trace/alive` | Keepalive signal | ✅ |
| POST | `/trace/workflow` | Submit workflow trace | ✅ |
| POST | `/trace/task` | Submit task trace | ✅ |
| POST | `/trace/init` | Initialize workflow | ✅ |

#### Current API (Active)
| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| POST | `/trace/create` | Create workflow trace | TraceCreateRequest | TraceCreateResponse |
| PUT | `/trace/{workflowId}/begin` | Workflow execution start | TraceBeginRequest | TraceBeginResponse |
| PUT | `/trace/{workflowId}/complete` | Workflow completion | TraceCompleteRequest | TraceCompleteResponse |
| PUT | `/trace/{workflowId}/progress` | Task progress updates | TraceProgressRequest | TraceProgressResponse |
| PUT | `/trace/{workflowId}/heartbeat` | Heartbeat signal | TraceHeartbeatRequest | TraceHeartbeatResponse |

**Trace Workflow:**
```
1. POST /trace/create → Get workflowId
2. PUT /trace/{workflowId}/begin → Start workflow
3. PUT /trace/{workflowId}/progress → Update tasks (multiple)
4. PUT /trace/{workflowId}/heartbeat → Keepalive
5. PUT /trace/{workflowId}/complete → Finish workflow
```

**TraceProgressRequest:**
- `progress` (TraceProgressData) - Workflow-level stats
- `tasks` (List<Task>, max 100) - Task updates

**Live Events Integration:**
- Publishes to `LiveEventsService` after each trace operation
- Triggers Server-Sent Events (SSE) to connected clients

---

### Real-Time Updates (`/live`)
**Controller:** `LiveEventsController.groovy`
**Security:** `IS_AUTHENTICATED`

| Method | Endpoint | Description | Protocol |
|--------|----------|-------------|----------|
| GET | `/live/` | Subscribe to live updates | Server-Sent Events (SSE) |

**Configuration:**
```yaml
live:
  buffer:
    time: 5s        # Buffer duration
    count: 100      # Max events per buffer
    heartbeat: 50s  # Heartbeat interval
```

**Event Types:**
- Workflow state changes
- Task progress updates
- Workflow completion notifications

**Implementation:**
- Uses RxJava Flowable for reactive streaming
- Buffered event delivery
- Automatic reconnection support

---

## Data Transfer Objects (DTOs)

### DTO Organization

All DTOs implement `BaseResponse` interface:
```groovy
interface BaseResponse {
    String getMessage()
}
```

**Package Structure:**
```
io.seqera.tower.exchange/
├── BaseResponse.groovy              # Base interface
├── MessageResponse.groovy           # Generic message
├── gate/                            # 2 DTOs
├── token/                           # 4 DTOs
├── user/                            # 4 DTOs
├── workflow/                        # 11 DTOs
├── trace/                           # 18 DTOs
├── task/                            # 2 DTOs
├── progress/                        # 2 DTOs
├── live/                            # 1 DTO
├── serviceinfo/                     # 1 DTO
└── captcha/                         # 1 DTO
```

**Total DTOs: 49 classes**

---

## Authentication & Security

### Authentication Methods

**Three Authentication Providers:**

1. **AuthenticationByApiToken**
   - Header: `Authorization: Bearer <api-token>`
   - Identity: `@token`
   - Validates against `AccessToken` table
   - Updates last-used timestamp asynchronously

2. **AuthenticationByMailAuthToken**
   - Email-based temporary tokens
   - Used for account verification
   - Time-limited validity

3. **AuthenticationByAdminPassword**
   - Admin login via password
   - Optional configuration

### JWT Configuration

```yaml
micronaut:
  security:
    enabled: true
    token:
      jwt:
        enabled: true
        bearer:
          enabled: false
        cookie:
          enabled: true
          loginSuccessTargetUrl: "/auth?success=true"
          loginFailureTargetUrl: "/auth?success=false"
        signatures:
          secret:
            generator:
              secret: pleaseChangeThisSecretForANewOne
        generator:
          access-token-expiration: 86400  # 24 hours
```

### Security Rules

**Annotation-Based Authorization:**
```groovy
@Secured(['ROLE_USER'])          // Requires authenticated user
@Secured(['ADMIN'])              // Requires admin role
@Secured(SecurityRule.IS_AUTHENTICATED)  // Any authenticated user
@Secured(SecurityRule.IS_ANONYMOUS)      // Public access
```

---

## Service Layer Architecture

### Core Services

**WorkflowService:**
```groovy
interface WorkflowService {
    String createWorkflowKey()
    Workflow get(String id)
    Workflow createWorkflow(TraceBeginRequest, User)
    Workflow updateWorkflow(Workflow, List<WorkflowMetrics>)
    List<Workflow> listByOwner(User, Long max, Long offset, String sqlRegex)
    void delete(Workflow)
    boolean markForDeletion(String workflowId)
    List<Workflow> findMarkedForDeletion(int max)
    List<WorkflowMetrics> findMetrics(Workflow)
    List<WorkflowComment> getComments(Workflow)
    List<String> getProcessNames(Workflow)
}
```

**TraceService:**
```groovy
interface TraceService {
    Workflow handleFlowBegin(TraceBeginRequest, User)
    Workflow handleFlowComplete(TraceCompleteRequest, User)
    void handleTaskTrace(String workflowId, TraceProgressData, List<Task>)
    void handleHeartbeat(String workflowId, TraceProgressData)
}
```

**UserService:**
```groovy
interface UserService {
    User create(String email)
    User getOrCreate(String email)
    User getByAuth(Principal)
    User getByEmail(String email)
    User getByAccessToken(String token)
    User update(User existing, User updated)
    void delete(User)
    List<String> findAuthoritiesByUser(User)
    boolean updateLastAccessTime(Long userId)
}
```

---

## Domain Model (Database Entities)

### Core Entities

```
┌─────────────┐
│    User     │
├─────────────┤
│ id (PK)     │───┐
│ userName    │   │
│ email       │   │
│ authToken   │   │
│ firstName   │   │
│ lastName    │   │
│ trusted     │   │
│ disabled    │   │
└─────────────┘   │
                  │ hasMany
                  ▼
     ┌────────────────────┐
     │   Workflow         │
     ├────────────────────┤
     │ id (PK)            │───┐
     │ owner_id (FK)      │   │
     │ sessionId          │   │
     │ status             │   │
     │ submit, start      │   │
     │ complete           │   │
     │ commandLine        │   │
     │ duration           │   │
     └────────────────────┘   │ hasMany
                              ▼
                 ┌──────────────────┐
                 │      Task        │
                 ├──────────────────┤
                 │ id (PK)          │
                 │ workflow_id (FK) │
                 │ taskId           │
                 │ status           │
                 │ data_id (FK)     │───┐
                 └──────────────────┘   │
                                        ▼
                           ┌────────────────────┐
                           │     TaskData       │
                           ├────────────────────┤
                           │ id (PK)            │
                           │ hash, name         │
                           │ submit, start      │
                           │ cpus, memory       │
                           │ duration, realtime │
                           │ exitStatus         │
                           │ container          │
                           │ executor           │
                           └────────────────────┘
```

### Complete Entity List

| Entity | Description | Key Relationships |
|--------|-------------|-------------------|
| **User** | User accounts | → Workflows, AccessTokens |
| **Workflow** | Workflow executions | → User (owner), Tasks |
| **Task** | Individual tasks | → Workflow, TaskData |
| **TaskData** | Task metrics/metadata | ← Task (1:1) |
| **AccessToken** | API tokens | → User |
| **WorkflowComment** | User comments | → Workflow, User (author) |
| **WorkflowMetrics** | Workflow stats | → Workflow |
| **WorkflowKey** | Workflow ID generation | Standalone |
| **Role** | User roles | ← UserRole |
| **UserRole** | User-Role mapping | → User, Role |
| **WfManifest** | Workflow manifest | Embedded in Workflow |
| **WfNextflow** | Nextflow version info | Embedded in Workflow |
| **WfStats** | Workflow statistics | Embedded in Workflow |
| **ProcessLoad** | Process load metrics | → Workflow |
| **WorkflowLoad** | Workflow load cache | → Workflow |
| **Mail** | Email queue | ← MailAttachment |
| **MailAttachment** | Email attachments | → Mail |

**Total Entities: 21 classes**

---

## API Usage Examples

### Complete Workflow Lifecycle

```bash
# 1. User Registration
curl -X POST http://localhost:8080/gate/access \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","captcha":"..."}'

# Response: { "user": {...}, "token": "jwt-token" }

# 2. Create API Token
curl -X POST http://localhost:8080/token/create \
  -H "Authorization: Bearer jwt-token" \
  -d '{"name":"my-cli-token"}'

# Response: { "token": "api-token-40-chars" }

# 3. Create Workflow
curl -X POST http://localhost:8080/trace/create \
  -H "Authorization: Bearer api-token" \
  -d '{}'

# Response: { "workflowId": "abc123xyz" }

# 4. Begin Workflow
curl -X PUT http://localhost:8080/trace/abc123xyz/begin \
  -H "Authorization: Bearer api-token" \
  -d '{
    "workflow": {
      "id": "abc123xyz",
      "sessionId": "uuid",
      "runName": "my-run",
      "commandLine": "nextflow run workflow.nf"
    }
  }'

# 5. Submit Task Progress
curl -X PUT http://localhost:8080/trace/abc123xyz/progress \
  -H "Authorization: Bearer api-token" \
  -d '{
    "progress": {
      "running": 5,
      "succeeded": 10
    },
    "tasks": [...]
  }'

# 6. Complete Workflow
curl -X PUT http://localhost:8080/trace/abc123xyz/complete \
  -H "Authorization: Bearer api-token" \
  -d '{
    "workflow": {
      "id": "abc123xyz",
      "success": true,
      "duration": 60000
    }
  }'

# 7. Get Workflow Details
curl http://localhost:8080/workflow/abc123xyz \
  -H "Authorization: Bearer api-token"

# 8. List All Workflows
curl "http://localhost:8080/workflow/list?max=10&offset=0&search=my" \
  -H "Authorization: Bearer api-token"
```

---

## Key File Locations

### Controllers
```
tower-backend/src/main/groovy/io/seqera/tower/controller/
├── BaseController.groovy
├── ServiceInfoController.groovy
├── GateController.groovy
├── TokenController.groovy
├── UserController.groovy
├── WorkflowController.groovy
├── TraceController.groovy
└── LiveEventsController.groovy
```

### Services
```
tower-backend/src/main/groovy/io/seqera/tower/service/
├── TowerService.groovy
├── GateService.groovy
├── UserService.groovy
├── AccessTokenService.groovy
├── WorkflowService.groovy
├── TraceService.groovy
├── TaskService.groovy
├── ProgressService.groovy
├── auth/
│   ├── AuthenticationByApiToken.groovy
│   ├── AuthenticationByMailAuthToken.groovy
│   └── AuthenticationByAdminPassword.groovy
└── live/
    └── LiveEventsService.groovy
```

### Domain Models
```
tower-backend/src/main/groovy/io/seqera/tower/domain/
├── User.groovy
├── Workflow.groovy
├── Task.groovy
├── TaskData.groovy
├── AccessToken.groovy
├── WorkflowComment.groovy
├── WorkflowMetrics.groovy
└── ... (21 total entities)
```

### DTOs
```
tower-backend/src/main/groovy/io/seqera/tower/exchange/
├── BaseResponse.groovy
├── gate/*.groovy (2 files)
├── token/*.groovy (4 files)
├── user/*.groovy (4 files)
├── workflow/*.groovy (11 files)
├── trace/*.groovy (18 files)
└── ... (49 total DTOs)
```

---

## Summary

### Key Metrics:
- **8 Controllers** managing **46+ endpoints**
- **26 Service classes** implementing business logic
- **21 Domain entities** persisted via GORM
- **49 DTO classes** for request/response mapping
- **3 Authentication methods** (JWT, API token, email auth)
- **Real-time updates** via Server-Sent Events
- **Multi-database support** (MySQL, H2, PostgreSQL, Oracle)

### API Design Principles:
✅ RESTful architecture
✅ Resource-based organization
✅ JWT + API token authentication
✅ Role-based access control
✅ Comprehensive error handling
✅ Real-time event streaming
✅ OpenAPI 3.0 documentation
✅ Transactional consistency

---

## Support

* For common problems, doubts and feedback please use the [Gitter community channel](https://gitter.im/nf-tower/community)
  or the [GitHub issues page](https://github.com/seqeralabs/nf-tower/issues).
* This source code is distributed as it is for community adoption.
* Distribution packages, deployment scripts, maintenance updates, migration scripts and custom integrations are available to customers of [Seqera Labs](https://seqera.io/).
 

## License

[Mozilla Public License v2.0](LICENSE.txt)
