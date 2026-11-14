# Phase 1 — Database Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [x]`) syntax.

**Goal:** Add the five remaining tables with real MySQL primary/foreign keys, seed demonstration master data, and prove the constraints directly against MySQL.

**Architecture:** Flyway owns the schema. `users` already exists from Phase 0. Phase 1 adds `pelanggan`, `barang`, `transaksi`, `detail_transaksi`, `identitas_instansi` in dependency order, then seeds master data. No Java entities or UI yet; they belong to Phase 2.

**Tech Stack:** MySQL 8.4.3 (Laragon), Flyway 12, Spring Boot 4.1.2, JUnit 5, JdbcTemplate.

**Spec:** User-approved revised plan (conversation, 2026-11-14).

## Global Constraints

- No Docker.
- All FKs enforced by MySQL, `ON DELETE RESTRICT` for used master data, `ON DELETE CASCADE` for `detail_transaksi`.
- No transaction deletion feature; database integrity only.
- No hardcoded letterhead data in code; `identitas_instansi` is the source.
- Do not start Phase 2.
- Do not claim success without `mvnw.cmd test` output.

## Review Focus

- Deleting a customer/user/barang that is referenced by a transaction must be refused by MySQL, not by application code.
- Deleting a transaction must remove its details automatically.
- Stock must never be negative in the schema.
- Seed data must be findable through SQL and usable by later phases.
- Flyway must apply cleanly on a database that already has `users`.

## File Structure

- `src/main/resources/db/migration/V3__create_pelanggan_table.sql` — customers.
- `src/main/resources/db/migration/V4__create_barang_table.sql` — goods + stock.
- `src/main/resources/db/migration/V5__create_transaksi_table.sql` — sales header + FKs.
- `src/main/resources/db/migration/V6__create_detail_transaksi_table.sql` — sales lines + FK rules.
- `src/main/resources/db/migration/V7__create_identitas_instansi_table.sql` — letterhead identity.
- `src/main/resources/db/migration/V8__seed_master_data.sql` — identity, customers, goods.
- `src/test/java/com/example/penjualan/Phase1DatabaseTest.java` — schema, FK behavior, seed.

### Task 1: Failing constraint tests

**Files:**
- Create: `src/test/java/com/example/penjualan/Phase1DatabaseTest.java`

**Interfaces:**
- Produces: proof that the six tables, four FKs, delete rules, and seeds exist.

- [x] **Step 1: Write `Phase1DatabaseTest`**

Tests:
1. `allSixTablesExist` — information_schema has exactly the six expected tables.
2. `everyTableHasPrimaryKey` — each table has a `PRIMARY KEY`.
3. `foreignKeysUseExpectedDeleteRules` — four FKs: `fk_transaksi_pelanggan` RESTRICT, `fk_transaksi_user` RESTRICT, `fk_detail_transaksi` CASCADE, `fk_detail_barang` RESTRICT.
4. `seedMasterDataLoaded` — at least 5 customers, at least 8 goods, exactly 1 identity, and `BRG-001` exists.
5. `deletingCustomerWithTransactionIsRestricted` — insert temp customer + transaction, delete customer, expect `DataIntegrityViolationException`.
6. `deletingTransactionCascadesToDetail` — insert temp transaction + detail, delete transaction, detail count is 0.
7. `deletingBarangUsedByDetailIsRestricted` — insert temp goods + detail, delete goods, expect `DataIntegrityViolationException`.

Constraint tests are `@Transactional` so all temporary rows roll back.

- [x] **Step 2: Run the test to verify RED**

Run: `.\mvnw.cmd test -Dtest=Phase1DatabaseTest`
Expected: failures because the five tables and four FKs do not exist.

### Task 2: Migrations

**Files:**
- Create: V3–V8 migration files listed above.

- [x] **Step 1: Create the five tables in dependency order**

`pelanggan` columns: `id_pelanggan` PK AI, `nama` NOT NULL, `alamat`, `no_telepon`, `email`, `created_at`.
`barang` columns: `id_barang` PK AI, `kode_barang` UNIQUE, `nama_barang`, `satuan`, `harga DECIMAL(15,2)`, `stok INT`, `stok_minimum INT`, `created_at`; CHECK stock and price >= 0.
`transaksi` columns: `id_transaksi` PK AI, `id_pelanggan` FK RESTRICT, `id_user` FK RESTRICT, `tanggal DATETIME`, `total DECIMAL(15,2)`; CHECK total >= 0.
`detail_transaksi` columns: `id_detail` PK AI, `id_transaksi` FK CASCADE, `id_barang` FK RESTRICT, `jumlah INT`, `harga_satuan DECIMAL(15,2)`, `subtotal DECIMAL(15,2)`; CHECK jumlah > 0.
`identitas_instansi` columns: `id_instansi` PK AI, `nama_instansi`, `alamat`, `email`, `no_telepon`, `logo`, `updated_at`.

All tables InnoDB, utf8mb4/utf8mb4_unicode_ci.

- [x] **Step 2: Seed demonstration data**

One identity, five customers, eight goods; goods include one item below `stok_minimum` for Phase 4.

- [x] **Step 3: Run the test to verify GREEN**

Run: `.\mvnw.cmd test`
Expected: all Phase 0 and Phase 1 tests pass.

### Task 3: Verify against MySQL and commit

- [x] **Step 1: Show all tables and constraints**

Run: `mysql -u root db_sistem_penjualan -e "SHOW CREATE TABLE ..."` for the five new tables.
Run: query `information_schema.REFERENTIAL_CONSTRAINTS` for the four FK rules.
Run: counts for each seed table.

- [x] **Step 2: Commit**

`git add` migrations and test, commit `feat: phase 1 database schema and constraints`.

- [x] **Step 3: Stop**

Report exact output; do not start Phase 2.
