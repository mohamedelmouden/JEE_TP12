# Securing a REST API with JWT — Spring Boot 3

> Practical work on stateless authentication using JSON Web Tokens (JWT) with Spring Security 6.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [API Reference](#api-reference)
- [Authentication Flow](#authentication-flow)
- [JWT Token Structure](#jwt-token-structure)
- [Testing with Postman](#testing-with-postman)
- [Common Errors](#common-errors)
- [Key Concepts](#key-concepts)

---

## Overview

This project demonstrates how to secure a REST API using the JWT (JSON Web Token) standard. Unlike session-based authentication, this approach is fully **stateless** — the server stores no authentication data. Every request carries a signed token that proves the identity and role of the caller.

**Learning outcomes:**
- Build an authentication endpoint that issues JWT tokens
- Implement a custom authorization filter
- Understand Spring Security's stateless model
- Protect endpoints by role (`ROLE_USER`, `ROLE_ADMIN`)

---

## Tech Stack

| Technology       | Version   | Purpose                              |
|------------------|-----------|--------------------------------------|
| Java             | 21   | Language                             |
| Spring Boot      | 3.       | Application framework                |
| Spring Security  | 6.       | Authentication & authorization       |
| Spring Web       | —         | REST endpoint exposure               |
| Spring Data JPA  | —         | Database access layer                |
| MySQL            | —         | Relational database                  |
| Lombok           | —         | Boilerplate reduction                |
| jjwt             | 0.11.5    | JWT generation and validation        |

---

## Project Structure

```
fst.elmouden.demonstration
 ├── config/
 │   ├── SecurityConfig.java          # Spring Security configuration
 │   └── DataInitializer.java         # Seed data on startup
 ├── entities/
 │   ├── User.java                    # User JPA entity
 │   └── Role.java                    # Role JPA entity
 ├── jwt/
 │   ├── JwtUtil.java                 # Token generation & validation
 │   └── JwtAuthorizationFilter.java  # Per-request JWT filter
 ├── repositories/
 │   ├── UserRepository.java
 │   └── RoleRepository.java
 ├── services/
 │   └── CustomUserDetailsService.java
 ├── web/
 │   ├── AuthController.java          # Login & register endpoints
 │   └── TestController.java          # Protected test endpoints
 └── DemonstrationApplication.java
```

---

## Getting Started

### Prerequisites

- Java 17 or 21
- Maven
- MySQL running locally on port `3306`

### Setup

**1. Create the database**

```sql
CREATE DATABASE elmouden_security;
```

**2. Configure the application**

Update `src/main/resources/application.properties` with your credentials (see [Configuration](#configuration)).

**3. Run the application**

```bash
mvn spring-boot:run
```

On first startup, `DataInitializer` automatically seeds the database with two users:

| Username | Password | Role         |
|----------|----------|--------------|
| `admin`  | `1234`   | `ROLE_ADMIN` |
| `user`   | `1234`   | `ROLE_USER`  |

---

## Configuration

`src/main/resources/application.properties`

```properties
server.port=8090

spring.datasource.url=jdbc:mysql://localhost:3306/elmouden_security?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT settings
jwt.secret=MySuperSecretKeyForJwtAuthentication123456
# Token validity in milliseconds (1 hour)
jwt.expiration=3600000
```

> **Note:** Inline comments are not supported in `.properties` files.
> Always place comments on their own line, or switch to `application.yml`.

---

## Architecture

### Request lifecycle

```
Client Request
      │
      ▼
JwtAuthorizationFilter          ← Extracts and validates the Bearer token
      │
      ▼
SecurityContextHolder           ← Injects authenticated user + roles
      │
      ▼
SecurityConfig (rules)          ← Checks role-based access rules
      │
      ├── Allowed ──▶ Controller ──▶ Response
      │
      └── Denied  ──▶ 403 Forbidden
```

### Component responsibilities

| Component                   | Responsibility                                                   |
|-----------------------------|------------------------------------------------------------------|
| `JwtUtil`                   | Generate, parse, and validate JWT tokens                        |
| `JwtAuthorizationFilter`    | Intercept each request, extract and validate the token          |
| `CustomUserDetailsService`  | Load user and roles from the database for Spring Security       |
| `SecurityConfig`            | Define session policy, CSRF, and route authorization rules      |
| `AuthController`            | Expose public `/login` and `/status` endpoints                  |
| `DataInitializer`           | Seed default roles and users on application startup             |

---

## API Reference

### Public endpoints

#### `POST /api/auth/login`

Authenticate a user and receive a JWT token.

**Request body:**
```json
{
  "username": "admin",
  "password": "1234"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "roles": "[ROLE_ADMIN]",
  "type": "Bearer",
  "expiresIn": "3600s"
}
```

**Response `401 Unauthorized`:**
```json
{
  "error": "Identifiants incorrects"
}
```

**Response `403 Forbidden`:**
```json
{
  "error": "Compte desactive, contactez l'administrateur"
}
```

---

#### `GET /api/auth/status`

Health check — verifies the API is running. No authentication required.

**Response `200 OK`:**
```json
{
  "status": "API operationnelle",
  "app": "spring-jwt-api"
}
```

---

### Protected endpoints

All protected endpoints require the following header:

```
Authorization: Bearer <token>
```

| Method | Endpoint               | Required Role             |
|--------|------------------------|---------------------------|
| GET    | `/api/user/profile`    | `ROLE_USER`, `ROLE_ADMIN` |
| GET    | `/api/admin/dashboard` | `ROLE_ADMIN` only         |

**Missing or invalid token → `403 Forbidden`**

---

## Authentication Flow

```
1. Client sends POST /api/auth/login with credentials
       │
2. AuthController calls AuthenticationManager
       │
3. Spring Security loads user via CustomUserDetailsService
       │
4. BCrypt compares the provided password with the stored hash
       │
5. If valid → JwtUtil generates a signed token (HS256)
       │
6. Token is returned to the client
       │
7. Client sends the token in subsequent requests:
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
       │
8. JwtAuthorizationFilter validates the token on each request
       │
9. If valid → SecurityContext is populated → access granted
```

---

## JWT Token Structure

A JWT consists of three Base64-encoded parts separated by `.`:

```
eyJhbGciOiJIUzI1NiJ9  .  eyJzdWIiOiJhZG1pbiJ9  .  G7C6xyz...
       HEADER                    PAYLOAD               SIGNATURE
```

| Part      | Content                                          |
|-----------|--------------------------------------------------|
| Header    | Signing algorithm (`HS256`)                      |
| Payload   | `sub` (username), roles, `iat`, `exp`            |
| Signature | HMAC-SHA256 of header + payload using the secret |

**Decoded payload example:**
```json
{
  "sub": "admin",
  "roles": ["ROLE_ADMIN"],
  "app": "spring-jwt-api",
  "iat": 1774719521,
  "exp": 1774723121
}
```

> The payload is readable by anyone. Never store passwords or sensitive data inside a JWT.

You can inspect any token at [jwt.io](https://jwt.io).

---

## Testing with Postman

### Step 1 — Login

```
POST http://localhost:8090/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "1234"
}
```

Copy the value of the `token` field from the response.

---

### Step 2 — Access a protected endpoint

```
GET http://localhost:8090/api/user/profile
Authorization: Bearer <paste_token_here>
```

Expected response: `200 OK`

---

### Step 3 — Access admin endpoint

```
GET http://localhost:8090/api/admin/dashboard
Authorization: Bearer <paste_token_here>
```

Expected response: `200 OK` (only if the user has `ROLE_ADMIN`)

---

### Step 4 — Access without token

```
GET http://localhost:8090/api/user/profile
```

Expected response: `403 Forbidden`

---

## Common Errors

| Error                                        | Cause                                                     | Fix                                                |
|----------------------------------------------|-----------------------------------------------------------|----------------------------------------------------|
| `Failed to convert "3600000 # 1 heure"`      | Inline comment in `.properties` file                      | Move the comment to its own line                   |
| `UserDetailsService bean could not be found` | `CustomUserDetailsService` missing or not annotated       | Add `@Service` to the class                        |
| `RoleRepository.save() not found`            | `RoleRepository` declared as a class, not an interface    | Use `interface` and extend `JpaRepository`         |
| `401 Unauthorized` on login                  | User does not exist or password mismatch                  | Check database or re-run `DataInitializer`         |
| `403 Forbidden` with a valid token           | Role insufficient or token from a different secret        | Re-login to get a fresh token                      |
| `SignatureException`                         | `jwt.secret` changed between token generation and check   | Ensure the secret is consistent in `.properties`   |
| Tables empty after startup                   | Wrong database name in `application.properties`           | Align the database name with the one in MySQL      |

---

## Key Concepts

| Concept             | Description                                                                      |
|---------------------|----------------------------------------------------------------------------------|
| **Spring Security** | Framework managing authentication and authorization in Spring applications       |
| **Stateless**       | No server-side session; each request is fully self-contained                     |
| **JWT**             | Digitally signed token carrying identity and authorization claims                |
| **Bearer Token**    | Token transmitted in the `Authorization` HTTP header                             |
| **Filter Chain**    | Ordered sequence of filters applied by Spring Security to every incoming request |
| **BCrypt**          | Password hashing algorithm — never store passwords in plain text                 |
| **HMAC-SHA256**     | Signing algorithm used to guarantee the integrity of the JWT                     |
| **SecurityContext** | Spring's holder for the currently authenticated user during a request            |


# le test :
<img width="960" height="540" alt="test 1" src="https://github.com/user-attachments/assets/55aa4b51-bf67-48ec-85f9-6bef1bbfdd8f" />

<img width="960" height="540" alt="test 2" src="https://github.com/user-attachments/assets/86a185f1-5a95-47a2-b2ab-013105fe1662" />
<img width="960" height="540" alt="test 3" src="https://github.com/user-attachments/assets/d42d8772-d75a-4e69-a4ca-980b5ad75ca6" />

<img width="960" height="540" alt="test 4 token invalide" src="https://github.com/user-attachments/assets/40169873-651f-4314-82f8-f8d279c7051d" />





