# Phase 2 — Data Master Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [x]`) syntax.

**Goal:** Authenticated admins can list, search, paginate, create, edit, and delete customers and goods, with server-side validation and a friendly error when MySQL refuses a delete because the row is used by a transaction.

**Architecture:** Spring MVC controllers call Spring Data repositories directly; no service layer for plain CRUD. JPA entities mirror the existing Flyway schema under `ddl-auto=validate`. Thymeleaf lists and forms reuse the Phase 0 layout, with one shared pagination fragment. Deletes are POST and rely on the database FK as the source of truth, translating `DataIntegrityViolationException` into a flash error.

**Tech Stack:** Spring Boot 4.1.2, Spring Data JPA, Jakarta Validation, Thymeleaf, Bootstrap 5 WebJars, MySQL 8.4.3.

**Spec:** User-approved revised plan (conversation, 2026-11-14).

## Global Constraints

- No Docker.
- Keep Phase 0/1 features working; do not edit migrations from Phase 1.
- All pages require login.
- Search and pagination must query the database, not filter an in-memory list.
- Foreign Key violations are reported as a message, never as a stack trace or 500.
- No fake data or placeholder statistics.
- Do not start Phase 3.

## Review Focus

- A blank required field must not insert a row.
- A duplicate `kode_barang` must show a field error, not a 500.
- Non-numeric price/stock input must show a readable error.
- Deleting a customer or barang used by a transaction must keep the row.
- Page/search links must preserve the search term.

## Decisions

| Item | Value |
|---|---|
| Page size | 10 |
| Customer sort | `nama` ascending |
| Goods sort | `kodeBarang` ascending |
| Customer search | `nama` contains, case-insensitive |
| Goods search | `kodeBarang` or `namaBarang` contains, case-insensitive |
| Create/update | POST `/pelanggan`, POST `/pelanggan/{id}`, POST `/barang`, POST `/barang/{id}` |
| Delete | POST `/pelanggan/{id}/delete`, POST `/barang/{id}/delete` |
| Duplicate code message | `Kode barang sudah digunakan.` |
| FK customer message | `Pelanggan tidak dapat dihapus karena sudah dipakai pada transaksi.` |
| FK goods message | `Barang tidak dapat dihapus karena sudah dipakai pada transaksi.` |
| Success messages | `Pelanggan berhasil ditambahkan/diperbarui/dihapus.` and the same for `Barang`. |

## File Structure

- `src/main/java/com/example/penjualan/entity/Pelanggan.java`
- `src/main/java/com/example/penjualan/entity/Barang.java`
- `src/main/java/com/example/penjualan/repository/PelangganRepository.java`
- `src/main/java/com/example/penjualan/repository/BarangRepository.java`
- `src/main/java/com/example/penjualan/controller/PelangganController.java`
- `src/main/java/com/example/penjualan/controller/BarangController.java`
- `src/main/resources/templates/pelanggan/list.html`
- `src/main/resources/templates/pelanggan/form.html`
- `src/main/resources/templates/barang/list.html`
- `src/main/resources/templates/barang/form.html`
- `src/main/resources/templates/layout/pagination.html`
- `src/main/resources/templates/layout/base.html` — flash alerts
- `src/main/resources/templates/layout/fragments.html` — enable master-data links
- `src/main/resources/static/css/app.css` — table/toolbar styles
- `src/main/resources/messages.properties` — type-mismatch messages
- `src/test/java/com/example/penjualan/Phase2PelangganTest.java`
- `src/test/java/com/example/penjualan/Phase2BarangTest.java`

### Task 1: Failing MockMvc tests

**Files:** the two test classes above.

- [x] **Step 1: Write `Phase2PelangganTest`**

Tests: list contains seed customer; search filters; page size 10 with 11 rows gives 2 pages; valid create inserts; blank name rejected with field error and no insert; valid update changes row; delete unused removes row; delete used by transaction keeps row and flashes the FK message.

- [x] **Step 2: Write `Phase2BarangTest`**

Tests: list contains `BRG-001`; search by code and by name; valid create; duplicate `BRG-001` rejected with field error; negative price rejected; valid update; delete unused; delete used by detail keeps row and flashes the FK message.

- [x] **Step 3: Run RED**

Run: `.\mvnw.cmd test -Dtest="Phase2*Test"`
Expected: failures because routes/views do not exist.

### Task 2: Entities and repositories

- [x] **Step 1: Add `Pelanggan` and `Barang` entities mapping the Phase 1 columns**

Validation rules: customer `nama` not blank/max 100, `alamat` max 255, `noTelepon` max 20, `email` valid/max 100. Goods `kodeBarang` not blank/max 20, `namaBarang` not blank/max 100, `satuan` not blank/max 20, `harga` not null/min 0, `stok` not null/min 0, `stokMinimum` not null/min 0. `createdAt` is read-only.

- [x] **Step 2: Add repositories**

`PelangganRepository`: `Page<Pelanggan> findByNamaContainingIgnoreCase(String nama, Pageable pageable)`.
`BarangRepository`: `Page<Barang> search(String q, Pageable pageable)`, `boolean existsByKodeBarang(String kode)`, `boolean existsByKodeBarangAndIdBarangNot(String kode, Long id)`.

### Task 3: Controllers

- [x] **Step 1: Add `PelangganController`** with list/search/pagination, create form, create, edit form, update, delete.

- [x] **Step 2: Add `BarangController`** with the same operations plus duplicate-code checks.

- [x] **Step 3: Translate FK failures**

Catch `DataIntegrityViolationException` only around delete and redirect with the catalogued `error` flash message.

### Task 4: Views

- [x] **Step 1: Add shared pagination fragment**

Preserve `q`; hide when `totalPages <= 1`; previous/next disabled at the edges.

- [x] **Step 2: Add customer and goods list pages**

Search toolbar, total count, bordered table, empty state, row actions, delete confirmation.

- [x] **Step 3: Add customer and goods forms**

Fields with `is-invalid` state, per-field messages, create/edit title from `pageTitle`, cancel link.

- [x] **Step 4: Wire layout**

Enable both sidebar links with active state, show `success`/`error` flash alerts in the base layout, add table toolbar styles.

### Task 5: Verify and commit

- [x] **Step 1: Run GREEN**

Run: `.\mvnw.cmd test`
Expected: Phase 0, 1, and 2 tests all pass.

- [x] **Step 2: Browser E2E**

Start the app, log in, create/edit/search/delete a customer, create a goods item, prove duplicate-code and FK delete messages appear, capture screenshots.

- [x] **Step 3: Commit and stop**

Commit `feat: phase 2 pelanggan and barang management`. Report exact output; do not start Phase 3.
