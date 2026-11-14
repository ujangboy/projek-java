# Phase 0 — Fondasi Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [x]`) syntax.

**Goal:** A Spring Boot app that starts locally, connects to MySQL Laragon, runs Flyway, protects the dashboard with BCrypt login, and renders a Bootstrap layout.

**Architecture:** Spring Boot 4.1.2, Java 21 bytecode on JDK 26, Maven Wrapper, Thymeleaf server-side rendering. Phase 0 creates only the `users` table; remaining tables arrive in Phase 1.

**Tech Stack:** Java 21 target, Spring Boot 4.1.2, Spring Security 7, Spring Data JPA, Flyway 12, MySQL 8.4.3 (Laragon), Thymeleaf, Bootstrap 5 WebJars.

**Spec:** User-approved revised plan (conversation, 2026-11-14).

## Global Constraints

- No Docker, Dockerfile, or docker-compose.
- MySQL `db_sistem_penjualan` on Laragon, user `root`, empty password (local only).
- No hardcoded institution identity.
- Do not implement Phase 1+ features in this phase.
- Do not claim success without running `mvnw.cmd test`.

## File Structure

- `pom.xml` — dependencies and Java 21 release.
- `src/main/resources/application.properties` — datasource, JPA validate, Flyway, upload limits.
- `src/main/resources/db/migration/V1__create_users_table.sql` — users table.
- `src/main/resources/db/migration/V2__seed_admin.sql` — admin seed.
- `src/main/java/com/example/penjualan/entity/User.java` — users entity.
- `src/main/java/com/example/penjualan/repository/UserRepository.java` — lookup by username.
- `src/main/java/com/example/penjualan/config/SecurityConfig.java` — filter chain + BCrypt.
- `src/main/java/com/example/penjualan/service/CustomUserDetailsService.java` — DB-backed login.
- `src/main/java/com/example/penjualan/controller/LoginController.java` — login page.
- `src/main/java/com/example/penjualan/controller/DashboardController.java` — dashboard placeholder.
- `src/main/java/com/example/penjualan/controller/GlobalModelAttributes.java` — current user in layout.
- `src/main/resources/templates/layout/base.html` — page fragment.
- `src/main/resources/templates/layout/fragments.html` — sidebar/topbar.
- `src/main/resources/templates/login.html` — login form.
- `src/main/resources/templates/dashboard/index.html` — welcome page.
- `src/main/resources/static/css/app.css` — shell styling.
- `src/test/java/com/example/penjualan/Phase0SmokeTest.java` — context, Flyway, admin, security.

### Task 1: Scaffold project

- [x] `git init`, local identity, `.gitignore`.
- [x] Generate project from Spring Initializr: Boot 4.1.2, Java 21, deps web/thymeleaf/data-jpa/validation/security/mysql/flyway.
- [x] Point Maven Wrapper to Maven 3.9.16.
- [x] Add WebJars: Bootstrap 5, Bootstrap Icons.
- [x] `mvnw.cmd -q -DskipTests package` succeeds.

### Task 2: Database login table

- [x] Write failing test: `users` table exists, admin user exists, BCrypt matches `admin123`.
- [x] Add `V1__create_users_table.sql` and `V2__seed_admin.sql`.
- [x] Run Flyway via `mvnw.cmd test`; constraints verified with `SHOW CREATE TABLE users`.
- [x] Commit.

### Task 3: Login

- [x] Write failing MockMvc tests: `/dashboard` redirects to `/login`; `/login` returns 200 and contains `Masuk`.
- [x] Add `User`, `UserRepository`, `CustomUserDetailsService`, `SecurityConfig`.
- [x] Add login page and dashboard placeholder.
- [x] Run tests; commit.

### Task 4: Layout

- [x] Add layout fragments, sidebar, topbar, CSS.
- [x] Verify rendered pages with Playwright: login page renders, login succeeds, dashboard renders, logout.
- [x] Commit.

### Task 5: Verification

- [x] `mvnw.cmd test` passes.
- [x] `SHOW CREATE TABLE users` confirms PK/unique and InnoDB.
- [x] `git status` clean.
- [x] Report exact commands and output; stop before Phase 1.
