# Learning Management System (LMS)
[![Java](https://img.shields.io/badge/Java-17-blue?logo=java&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-brightgreen?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-blue?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-blue?logo=docker&logoColor=white)](https://www.docker.com/)
[![build](https://github.com/Mazen-Abdelfattah/Learning-Management-System/actions/workflows/ci.yml/badge.svg)](https://github.com/Mazen-Abdelfattah/Learning-Management-System/actions)
![Redis](https://img.shields.io/badge/Redis-Caching-red)

A full-featured, production-ready Learning Management System built with Java Spring Boot. Supports role-based access for Admins, Instructors, and Students with course management, OTP-based attendance tracking, assessments, grading, and real-time notifications.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Caching Strategy](#caching-strategy)
- [OTP Attendance Flow](#otp-attendance-flow)
- [CI/CD Pipeline](#cicd-pipeline)
- [Testing](#testing)
- [Project Structure](#project-structure)

---

## Features

### User Management
- Three roles: **Admin**, **Instructor**, **Student**
- JWT-based authentication with role-based access control
- Profile management — view and update personal information

### Course Management
- Instructors can create and manage courses with title, description, and duration
- Media file uploads per lesson (PDF, video, audio, images, documents)
- Student enrollment with notifications to both students and instructors
- Lesson organization with ordered index per course

### Attendance Management
- Instructors generate a 6-digit OTP per lesson
- OTP stored in Redis with automatic 10-minute TTL expiry — no database writes for ephemeral data
- Students submit the OTP to mark attendance; duplicate submissions are rejected

### Assessment & Grading
- **Quizzes** — MCQ, True/False, and Short Answer question types
- **Question Bank** — per-course repository of reusable questions
- **Assignments** — file upload submissions reviewed by instructors
- Instructors grade assignments and provide written feedback
- Students receive automated feedback after quizzes

### Performance Tracking
- Instructors track quiz scores, assignment submissions, and attendance per student

### Notifications
- System notifications for enrollment confirmations, course updates, new lessons, and instructor additions
- Students and instructors can filter unread vs. all notifications

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.4.0 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Database | MySQL 8.0 |
| Cache | Redis 7 (Spring Data Redis) |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven |
| Testing | JUnit 5, Mockito |
| CI/CD | GitHub Actions + Docker + DockerHub |

---

## Architecture

The application follows a layered architecture:

```
Controller  →  Service  →  Repository  →  Database (MySQL)
                  ↕
              Redis Cache
```

- **Controllers** handle HTTP requests and delegate to services
- **Services** contain business logic; caching annotations live here
- **Repositories** are Spring Data JPA interfaces
- **DTOs** are used as cache values — never raw Hibernate entities — to avoid `LazyInitializationException` during deserialization
- **Redis** serves two purposes: `@Cacheable` for read-heavy endpoints, and `RedisTemplate` for OTP storage with TTL

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker + Docker Compose

### 1. Clone the Repository

```bash
git clone https://github.com/Mazen-Abdelfattah/Learning-Management-System.git
cd Learning-Management-System
```

### 2. Configure Environment Variables

Create a `.env` file in the project root:

```env
DB_USERNAME=lms_user
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret
```

> **Note:** `.env` is in `.gitignore` and should never be committed.

### 3. Start Infrastructure (MySQL + Redis)

```bash
docker-compose up -d mysql redis
```

This spins up MySQL on port `3307` and Redis on port `6379`. Both include health checks — the app won't start until they're ready.

### 4. Run the Application

**Option A — IntelliJ (recommended for development):**

Add the same env vars from `.env` to your IntelliJ Run Configuration:
`Run → Edit Configurations → Environment Variables`

Then run `LmsApplication` normally.

**Option B — Full Docker stack:**

```bash
docker-compose up --build
```

The API will be available at `http://localhost:8080`.

---

## Configuration

### application.properties

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/lms?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update

jwt.secret=${JWT_SECRET}

spring.data.redis.host=localhost
spring.data.redis.port=6379

file.upload.directory=./uploads
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### Environment Variables

| Variable | Description |
|---|---|
| `DB_USERNAME` | MySQL username |
| `DB_PASSWORD` | MySQL password |
| `JWT_SECRET` | Secret key for signing JWT tokens |

---

## Caching Strategy

Redis caching is applied to 3 high-traffic read endpoints using Spring's `@Cacheable` annotation. Custom Jackson serialization (`GenericJackson2JsonRedisSerializer`) is configured to handle type metadata correctly.

| Cache Name | Method | Key | TTL |
|---|---|---|---|
| `users` | `getUserById` | `#id` | 10 min |
| `lessons` | `getLessonsByCourse` | `#courseId` | 10 min |
| `enrolledStudents` | `findStudentEnrolledInCourse` | `#courseId` | 10 min |

`@CacheEvict` is applied on all corresponding create, update, and delete operations to keep the cache consistent with the database.

**Performance results (measured locally with seed data):**

| Endpoint | Before Redis | After Redis | Improvement |
|---|---|---|---|
| `GET /courses/{id}/lessons` | 101ms | 34ms | 66% faster |
| `GET /users/{id}` | 30ms | 32ms | — |
| `GET /courses/{id}/studentEnrolled` | 312ms | 49ms | 84% faster |

> Note: DTOs (`UserDto`, `LessonResponseDto`) are cached instead of raw Hibernate entities to prevent `LazyInitializationException` during deserialization without requiring eager loading.

---

## OTP Attendance Flow

OTPs are stored in Redis using `StringRedisTemplate` directly — not in MySQL — because they are short-lived and never queried historically.

```
Instructor calls generate-otp
    → 6-digit OTP generated
    → Stored in Redis as key: otp:lesson:{lessonId}
    → TTL: 10 minutes (auto-expires, no cleanup needed)

Student calls mark-attendance with OTP
    → Redis key checked: otp:lesson:{lessonId}
    → If null  → "No OTP generated or OTP expired"
    → If wrong → "Invalid OTP"
    → If correct → attendance record saved to MySQL
```

---

## CI/CD Pipeline

GitHub Actions workflow runs on every push and pull request to `main`.

### Pipeline Jobs

**Job 1 — Build & Test:**
1. Spins up a Redis service container (health-checked before tests run)
2. Sets up Java 17
3. Runs `mvn test` with credentials injected from GitHub Secrets
4. Builds the JAR with `mvn package -DskipTests`

**Job 2 — Docker Build & Push** *(runs on push to main only):*
1. Logs in to DockerHub
2. Builds Docker image from `Dockerfile`
3. Pushes image tagged as `latest` and with the commit SHA

### GitHub Secrets Required

| Secret | Purpose |
|---|---|
| `DB_USERNAME` | Database credentials for tests |
| `DB_PASSWORD` | Database credentials for tests |
| `JWT_SECRET` | JWT signing key for tests |
| `DOCKER_USERNAME` | DockerHub login |
| `DOCKER_PASSWORD` | DockerHub login |

---

## Testing

Unit tests cover the service layer using JUnit 5 and Mockito. No Spring context is loaded — all dependencies are mocked.

```bash
mvn test
```

Key test classes:
- `AttendanceServiceTest` — OTP generation, attendance marking, invalid/expired OTP, duplicate attendance
- `CourseServiceTest` — course creation, enrollment, instructor management
- `UserServiceTest` — user CRUD, role assignment

---

## Project Structure

```
src/
├── main/
│   └── java/org/software/lms/
│       ├── config/          # RedisConfig
│       ├── controller/      # REST controllers
│       ├── dto/             # Data Transfer Objects (UserDto, LessonResponseDto, ...)
│       ├── exception/       # ResourceNotFoundException, global handler
│       ├── model/           # JPA entities (User, Course, Lesson, Quiz, ...)
│       ├── repository/      # Spring Data JPA interfaces
│       ├── security/        # JWT filter, UserDetailsService
│       ├── service/         # Business logic + caching annotations
│       └── LmsApplication.java
│   └── resources/
│       └── application.properties
├── test/
│   └── java/org/software/lms/
│       └── service/         # Unit tests
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.