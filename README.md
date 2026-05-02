# ResidentOps — Apartment Governance & Operations Platform

## Technology Stack

| Layer      | Technology                              |
|------------|-----------------------------------------|
| Backend    | Java 17 + Spring Boot 3.2               |
| Database   | MySQL 8.x                               |
| ORM        | Spring Data JPA / Hibernate             |
| Frontend   | HTML5 + CSS3 + Vanilla JS (single page) |
| Build Tool | Maven 3.x                               |
| IDE        | IntelliJ IDEA (Community or Ultimate)   |

---

## Design Patterns & Principles Applied

### SOLID Principles
| Principle | Where Applied |
|-----------|---------------|
| **SRP** — Single Responsibility | `SLAService` only handles SLA. `AuditLogger` only logs. `RequestFactory` only creates. `User` only holds identity. |
| **OCP** — Open/Closed | `IEscalationStrategy`: add new channels (WhatsApp, push) without touching `SLAService`. `User` hierarchy extended via services. |
| **DIP** — Dependency Inversion | `SLAService` depends on `IEscalationStrategy` interface. `NotificationService` depends on `IObserver` interface. |

### GRASP Principles
| Principle | Where Applied |
|-----------|---------------|
| **Creator** | `RequestFactory` creates `MaintenanceRequest` because it owns the SLA initialization data. |
| **Information Expert** | `MaintenanceRequest.isSLABreached()` — object with data owns the query. `Poll.isExpired()` — same logic. |

### Design Patterns
| Pattern | Class | Justification |
|---------|-------|---------------|
| **Singleton** | `NotificationService`, `AuditLogger` | Single event dispatcher + single audit writer across app |
| **Factory Method** | `RequestFactory.createRequest()` | Decouples object creation; computes SLA deadline internally |
| **Observer** | `NotificationService` + `IObserver` | Fan-out event broadcasting; add observers without touching publisher |
| **Strategy** | `IEscalationStrategy`, `EmailEscalationStrategy`, `SMSEscalationStrategy` | Swap escalation channel via `@Qualifier` without changing `SLAService` |

---

## Folder Structure

```
residentops/
├── pom.xml                                         ← Maven build file
├── sql/
│   └── schema_and_seed.sql                         ← Run this in MySQL first
└── src/main/
    ├── resources/
    │   ├── application.properties                  ← DB config, server port
    │   └── static/
    │       └── index.html                          ← Full frontend SPA
    └── java/com/residentops/
        ├── ResidentOpsApplication.java             ← Spring Boot entry point
        ├── config/
        │   └── WebConfig.java                      ← CORS + static file config
        ├── model/
        │   ├── User.java
        │   ├── MaintenanceRequest.java
        │   ├── Complaint.java
        │   ├── Poll.java
        │   ├── Vote.java
        │   ├── Expense.java
        │   ├── Notice.java
        │   ├── Feedback.java
        │   ├── AuditLog.java
        │   └── enums/
        │       ├── Role.java
        │       ├── RequestStatus.java
        │       ├── Priority.java
        │       ├── RequestCategory.java
        │       ├── ComplaintType.java
        │       ├── ComplaintStatus.java
        │       ├── VoteChoice.java
        │       ├── PollStatus.java
        │       ├── NoticeType.java
        │       ├── ExpenseCategory.java
        │       └── OwnerType.java
        ├── repository/
        │   ├── UserRepository.java
        │   ├── MaintenanceRequestRepository.java
        │   ├── ComplaintRepository.java
        │   ├── PollRepository.java
        │   ├── VoteRepository.java
        │   ├── ExpenseRepository.java
        │   ├── NoticeRepository.java
        │   ├── FeedbackRepository.java
        │   └── AuditLogRepository.java
        ├── factory/
        │   └── RequestFactory.java                 ← Factory + Creator pattern
        ├── observer/
        │   ├── IObserver.java                      ← Observer interface
        │   └── ConsoleNotificationObserver.java    ← Concrete observer
        ├── strategy/
        │   ├── IEscalationStrategy.java            ← Strategy interface (DIP)
        │   ├── EmailEscalationStrategy.java        ← Concrete strategy
        │   └── SMSEscalationStrategy.java          ← Concrete strategy
        ├── singleton/
        │   ├── NotificationService.java            ← Singleton + Observer publisher
        │   └── AuditLogger.java                    ← Singleton audit writer
        ├── service/
        │   ├── MaintenanceService.java
        │   ├── ComplaintService.java
        │   ├── VotingService.java
        │   ├── ExpenseService.java
        │   ├── NoticeService.java
        │   ├── UserService.java
        │   └── SLAService.java                     ← Strategy injection + scheduler
        └── controller/
            ├── AuthController.java
            ├── MaintenanceController.java
            ├── ComplaintController.java
            ├── VotingController.java
            ├── ExpenseController.java
            ├── NoticeController.java
            └── AuditController.java
```

---

## Step-by-Step Setup in IntelliJ IDEA

### Prerequisites
- Java JDK 17+ installed
- MySQL 8.x installed and running
- IntelliJ IDEA (Community Edition is fine)
- Maven (bundled with IntelliJ)

---

### Step 1 — Set up MySQL Database

Open MySQL Workbench or any MySQL client and run:

```sql
CREATE DATABASE IF NOT EXISTS residentops;
```

Then run the full `sql/schema_and_seed.sql` file to create tables and insert demo data.

**Default credentials in `application.properties`:**
```
username = root
password = root
```

> **Change these** in `src/main/resources/application.properties` if your MySQL uses different credentials.

---

### Step 2 — Open Project in IntelliJ IDEA

1. Open IntelliJ IDEA
2. Click **File → Open**
3. Navigate to the `residentops/` folder and click **OK**
4. IntelliJ will detect the `pom.xml` — click **"Load Maven Project"** when prompted
5. Wait for Maven to download all dependencies (first time takes ~2–3 minutes)

---

### Step 3 — Configure Database Connection

Open `src/main/resources/application.properties` and update:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/residentops?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root        ← change this
spring.datasource.password=root        ← change this
```

---

### Step 4 — Run the Application

**Option A — From IntelliJ:**
1. Open `ResidentOpsApplication.java`
2. Click the green ▶ **Run** button next to `public static void main`
3. Or press **Shift + F10**

**Option B — From Maven terminal (IntelliJ terminal):**
```bash
mvn spring-boot:run
```

**Option C — Build JAR and run:**
```bash
mvn clean package -DskipTests
java -jar target/residentops-1.0.0.jar
```

---

### Step 5 — Open the Application

Once you see this in the console:
```
Started ResidentOpsApplication in X.XXX seconds
```

Open your browser and go to:
```
http://localhost:8080
```

---

## Demo Login Credentials

| Role             | Email                    | Password |
|------------------|--------------------------|----------|
| Resident         | arun@resident.com        | pass123  |
| Resident (2)     | priya@resident.com       | pass123  |
| Committee Admin  | ravi@admin.com           | pass123  |
| Committee Admin  | sunita@admin.com         | pass123  |
| Vendor (Plumber) | ramesh@vendor.com        | pass123  |
| Vendor (Elec.)   | suresh@vendor.com        | pass123  |
| Auditor          | auditor@ops.com          | pass123  |

---

## API Endpoints Reference

### Auth
| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/auth/login | Login |
| POST | /api/auth/register | Register new user |
| GET  | /api/auth/users | Get all users |
| GET  | /api/auth/users/role/{role} | Get by role |

### Maintenance Requests
| Method | URL | Description |
|--------|-----|-------------|
| GET  | /api/requests | All requests |
| POST | /api/requests/raise | Raise new request |
| POST | /api/requests/{id}/assign | Assign vendor |
| PUT  | /api/requests/{id}/status | Update status |
| POST | /api/requests/{id}/close | Close request |
| POST | /api/requests/{id}/reject | Reject request |
| POST | /api/requests/{id}/feedback | Submit feedback |
| POST | /api/requests/sla/check | Manual SLA check |

### Complaints
| Method | URL | Description |
|--------|-----|-------------|
| GET  | /api/complaints | All complaints |
| POST | /api/complaints/file | File complaint |
| PUT  | /api/complaints/{id}/status | Update status |

### Polls / Voting
| Method | URL | Description |
|--------|-----|-------------|
| GET  | /api/polls | All polls |
| POST | /api/polls/create | Create poll |
| POST | /api/polls/{id}/vote | Cast vote |
| GET  | /api/polls/{id}/results | Get results |
| POST | /api/polls/{id}/close | Close poll |

### Expenses
| Method | URL | Description |
|--------|-----|-------------|
| GET  | /api/expenses | All expenses |
| GET  | /api/expenses/total | Total amount |
| POST | /api/expenses/add | Add expense |

### Notices
| Method | URL | Description |
|--------|-----|-------------|
| GET  | /api/notices | All notices |
| GET  | /api/notices/emergencies | Emergency notices |
| POST | /api/notices/post | Post notice |

### Audit
| Method | URL | Description |
|--------|-----|-------------|
| GET  | /api/audit | Full audit trail |
| GET  | /api/audit/entity/{type} | Filter by entity |
| GET  | /api/audit/actor/{userId} | Filter by actor |

---

## Role-Based Access Summary

| Feature              | Resident | Committee Admin | Vendor | Auditor |
|----------------------|----------|-----------------|--------|---------|
| Raise request        | ✅       | —               | —      | —       |
| Assign vendor        | —        | ✅              | —      | —       |
| Update work status   | —        | —               | ✅     | —       |
| Close/reject request | —        | ✅              | —      | —       |
| Submit feedback      | ✅       | —               | —      | —       |
| File complaint       | ✅       | —               | —      | —       |
| Resolve complaint    | —        | ✅              | —      | —       |
| Create poll          | —        | ✅              | —      | —       |
| Cast vote            | ✅       | —               | —      | —       |
| Close poll           | —        | ✅              | —      | —       |
| Add expense          | —        | ✅              | —      | —       |
| View expenses        | —        | ✅              | —      | ✅     |
| Post notice          | —        | ✅              | —      | —       |
| View audit log       | —        | ✅              | —      | ✅     |

---

## Common Errors & Fixes

| Error | Fix |
|-------|-----|
| `Access denied for user 'root'@'localhost'` | Update username/password in `application.properties` |
| `Communications link failure` | Make sure MySQL is running: `sudo service mysql start` |
| `Port 8080 already in use` | Change `server.port=8081` in `application.properties` |
| `Table doesn't exist` | Run `sql/schema_and_seed.sql` in MySQL first |
| Lombok errors in IntelliJ | Go to **Settings → Plugins → Marketplace** → install **Lombok** plugin, then **Enable annotation processing** in **Settings → Build → Compiler → Annotation Processors** |

---

## What Was Added Beyond Requirements

1. **SLA auto-escalation scheduler** — runs every 5 min, auto-escalates breached requests using Strategy pattern
2. **Vendor performance rating** — automatically recalculated on every feedback submission
3. **Emergency broadcast** — notices marked as emergency appear as red banners on all dashboards
4. **Full audit trail** — every action (request raised, poll closed, expense added) is persisted with actor + timestamp
5. **Quorum enforcement** — polls auto-close as "INCONCLUSIVE" if minimum vote threshold not met
6. **Role-based UI** — frontend shows only the features allowed for the logged-in user's role
7. **SLA deadline color coding** — breached deadlines shown in red in the requests table
