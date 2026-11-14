# Phase 3 â€” Transaksi Penjualan Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Authenticated admins can create multi-item sales transactions with a price snapshot, stock validation under pessimistic locking, automatic rollback on failure, a newest-first transaction list, and a printable receipt.

**Architecture:** `TransaksiController` delegates creation to `TransaksiService`, the first service layer in the app because creation spans multiple rows and must be atomic. The service merges duplicate goods rows, locks the affected `barang` rows with `PESSIMISTIC_WRITE`, validates stock, snapshots prices, deducts stock, and inserts the header plus details in one transaction. Thymeleaf renders the list, form, and receipt; a small page script adds/removes item rows and shows the running total.

**Tech Stack:** Spring Boot 4.1.2, Spring Data JPA, Jakarta Validation, Thymeleaf, Bootstrap 5 WebJars, MySQL 8.4.3.

**Spec:** User-approved phase plan (conversation, 2026-11-14) plus the Phase 1 schema.

## Global Constraints

- No Docker.
- Keep Phase 0/1/2 features working; do not edit existing migrations.
- All pages require login.
- No fake data, no placeholder statistics.
- Transactions are never deleted; no delete route or button.
- Receipt shows the price snapshot, not the current goods price.
- Detail rows for the same goods in one submission are merged.
- Insufficient stock rejects the whole transaction; no partial stock change.
- Do not start Phase 4.

## Review Focus

- Submitting the same goods twice must produce one merged detail row.
- Two items where the second lacks stock must leave the first item's stock unchanged.
- Changing a goods price after a sale must not change the receipt or total.
- A non-numeric quantity must not return a 400/500 page.
- Concurrent sales must not oversell; guarded by `PESSIMISTIC_WRITE`.

## Decisions

| Item | Value |
|---|---|
| Page size | 10 |
| List sort | `tanggal` DESC, `idTransaksi` DESC |
| Number format | `TRX-YYYYMMDD-XXXX` from `tanggal` and zero-padded `idTransaksi` (`%04d`) |
| Routes | GET `/transaksi`, GET `/transaksi/new`, POST `/transaksi`, GET `/transaksi/{id}` |
| Success message | `Transaksi berhasil disimpan.` |
| Customer missing | `Pelanggan wajib dipilih.` |
| Customer not found | `Pelanggan tidak ditemukan.` |
| No items | `Minimal satu barang harus dipilih.` |
| Row without goods | `Barang wajib dipilih.` |
| Quantity below 1 | `Jumlah barang minimal 1.` |
| Quantity not numeric | `Jumlah barang harus berupa angka.` |
| Stock failure | `Stok barang '<nama>' tidak mencukupi. Tersedia: <stok>, diminta: <jumlah>.` |

## File Structure

- `src/main/java/com/example/penjualan/entity/Transaksi.java` â€” header, detail list, receipt number.
- `src/main/java/com/example/penjualan/entity/DetailTransaksi.java` â€” item line with snapshot.
- `src/main/java/com/example/penjualan/entity/IdentitasInstansi.java` â€” receipt letterhead.
- `src/main/java/com/example/penjualan/repository/TransaksiRepository.java` â€” list and receipt graphs.
- `src/main/java/com/example/penjualan/repository/IdentitasInstansiRepository.java` â€” first identity.
- `src/main/java/com/example/penjualan/repository/BarangRepository.java` â€” add locked lookup.
- `src/main/java/com/example/penjualan/dto/TransaksiForm.java` â€” customer + item rows.
- `src/main/java/com/example/penjualan/service/TransaksiService.java` â€” atomic creation.
- `src/main/java/com/example/penjualan/service/TransaksiException.java` â€” user-facing failure.
- `src/main/java/com/example/penjualan/controller/TransaksiController.java` â€” routes.
- `src/main/resources/templates/transaksi/list.html`, `form.html`, `detail.html`.
- `src/main/resources/static/js/transaksi-form.js`.
- `src/main/resources/static/css/app.css` â€” receipt/print styles.
- `src/main/resources/templates/layout/fragments.html` â€” enable Transaksi, footer Phase 3.
- `src/test/java/com/example/penjualan/Phase3TransaksiTest.java`.

### Task 1: Failing tests

**Files:** `src/test/java/com/example/penjualan/Phase3TransaksiTest.java`

Fixture IDs: customer `930001`; goods `930001` (price 1000, stock 10), `930002` (price 2500, stock 5). A `@BeforeEach` deletes leftovers and inserts fixtures; `@AfterEach` deletes `transaksi` rows for customer `930001` (details cascade), then goods/customer, then resets `AUTO_INCREMENT` for `transaksi`, `detail_transaksi`, `pelanggan`, `barang`.

Tests:

1. `numberUsesDateAndPaddedId` â€” entity `id=7`, date `2026-11-14 10:00` â†’ `TRX-20261114-0007`.
2. `listShowsTransactionsNewestFirst` â€” insert `930010` (13 Nov) and `930011` (14 Nov); page contains both and `930011` appears first.
3. `createSavesHeaderDetailsStockAndTotal` â€” POST customer + 2 goods; redirect `/transaksi/*`; flash success; total `4500.00`, 2 details, stock 8 and 4.
4. `duplicateRowsAreMerged` â€” same goods twice (1 + 2); one detail with `jumlah=3`, stock 7, total `3000.00`.
5. `insufficientStockRollsBackEverything` â€” good row (goods 930002 qty 1) then goods 930001 qty 999; form re-render with stock message; 0 transactions/details; stocks unchanged.
6. `missingCustomerIsRejected` â€” no `idPelanggan`; form + `Pelanggan wajib dipilih.`; 0 transactions.
7. `unknownCustomerIsRejected` â€” id `999999`; form + `Pelanggan tidak ditemukan.`.
8. `emptyItemsAreRejected` â€” no item params; form + `Minimal satu barang harus dipilih.`.
9. `zeroQuantityIsRejected` â€” qty `0`; form + `Jumlah barang minimal 1.`.
10. `nonNumericQuantityIsRejected` â€” qty `abc`; form + `Jumlah barang harus berupa angka.`; 0 transactions.
11. `receiptShowsInstitutionAndSnapshot` â€” create via service, change goods price to 9999, GET receipt; contains `Toko Sinar Jaya`, old `1.000,00`, total `2.000,00`, not `9.999,00`, `Cetak`.
12. `receiptForMissingTransactionIs404`.
13. `deleteRouteDoesNotExist` â€” POST `/transaksi/{id}/delete` â†’ 404 and row remains.

- [x] **Step 1: Write the tests above**
- [x] **Step 2: Run RED**

Run: `.\mvnw.cmd test -Dtest=Phase3TransaksiTest`
Expected: compilation failure for missing classes, then 404/500 failures after stubs exist. The first RED is the missing `Transaksi`/`getNomorTransaksi` compile error, acceptable for a new subsystem.

### Task 2: Entities and repositories

**Interfaces:**

- `Transaksi`: `Long getIdTransaksi()`, `String getNomorTransaksi()`, `Pelanggan getPelanggan()`, `User getUser()`, `LocalDateTime getTanggal()`, `BigDecimal getTotal()`, `List<DetailTransaksi> getDetails()`, `void addDetail(DetailTransaksi)`.
- `DetailTransaksi`: `Barang getBarang()`, `Integer getJumlah()`, `BigDecimal getHargaSatuan()`, `BigDecimal getSubtotal()`, setters.
- `IdentitasInstansi`: `getNamaInstansi()`, `getAlamat()`, `getEmail()`, `getNoTelepon()`, `getLogo()`.
- `TransaksiRepository`:
  - `@EntityGraph({"pelanggan","user"}) Page<Transaksi> findAllByOrderByTanggalDescIdTransaksiDesc(Pageable)`.
  - `@EntityGraph({"pelanggan","user","details","details.barang"}) Optional<Transaksi> findDetailById(Long)` via `@Query`.
- `IdentitasInstansiRepository`: `Optional<IdentitasInstansi> findFirstByOrderByIdInstansiAsc()`.
- `BarangRepository`: `@Lock(PESSIMISTIC_WRITE) @Query("select b from Barang b where b.idBarang in :ids order by b.idBarang") List<Barang> findAllForUpdate(Collection<Long> ids)`.

Mappings must match the Flyway columns exactly; `created_at`/`updated_at` are read-only.

- [x] **Step 1: Add the three entities**
- [x] **Step 2: Add/extend repositories**
- [x] **Step 3: Run `numberUsesDateAndPaddedId`**

Run: `.\mvnw.cmd test -Dtest=Phase3TransaksiTest#numberUsesDateAndPaddedId`
Expected: PASS.

### Task 3: Service and form DTO

**Interfaces:**

- `TransaksiForm`: `Long getIdPelanggan()`, `List<ItemForm> getItems()`; `ItemForm`: `Long getIdBarang()`, `Integer getJumlah()`.
- `TransaksiException extends RuntimeException`.
- `TransaksiService.create(TransaksiForm form, String username) -> Transaksi`, `@Transactional`.

Rules: trim/validate customer, reject empty items, reject null goods or quantity < 1, merge duplicates by first appearance, lock sorted ids, reject missing goods, reject stock shortfall, snapshot `harga`, subtotal, total, deduct stock, save with cascade.

- [x] **Step 1: Add DTO + exception**
- [x] **Step 2: Add service with the rules above**
- [x] **Step 3: Run the service-backed tests** after Task 4 exists; otherwise proceed.

### Task 4: Controller

**Interfaces:** `GET /transaksi` (pageable list), `GET /transaksi/new` (form + customer/goods lists), `POST /transaksi` (create, redirect detail, re-render form on `TransaksiException` or binding errors), `GET /transaksi/{id}` (receipt, 404 when missing).

- [x] **Step 1: Add controller**
- [x] **Step 2: Run `.\mvnw.cmd test -Dtest=Phase3TransaksiTest`**

Expected: API/DB tests pass once views exist; form/receipt tests need Task 5.

### Task 5: Views, script, styles, navigation

- [x] **Step 1: `transaksi/list.html`** â€” heading `Data Transaksi`, total count, table (Nomor, Tanggal, Pelanggan, Kasir, Total, Lihat), empty state `Belum ada transaksi.`, pager.
- [x] **Step 2: `transaksi/form.html`** â€” customer select, item rows, add/remove buttons, running total, inline error alert, `Simpan Transaksi`, cancel.
- [x] **Step 3: `transaksi/detail.html`** â€” institution letterhead, transaction metadata, item table, total, `Cetak` (`window.print()`), back link, thank-you line.
- [x] **Step 4: `transaksi-form.js`** â€” add/remove/reindex rows, recalc price/subtotal/total from option data attributes.
- [x] **Step 5: `app.css`** â€” receipt and `@media print` rules hiding sidebar/topbar/`.no-print`.
- [x] **Step 6: `fragments.html`** â€” Transaksi link active on `activeMenu == 'transaksi'`, footer `Phase 3` / `Transaksi`.
- [x] **Step 7: Run full suite**

Run: `.\mvnw.cmd test`
Expected: Phase 0â€“3 all green.

### Task 6: Verify, clean, commit

- [x] **Step 1: Browser E2E** â€” create a 2-item transaction, verify receipt, stock drop, merged duplicate rows, stock-error message, print CSS; screenshots.
- [x] **Step 2: Clean DB and reset AUTO_INCREMENT** â€” no test transactions/goods/customers left; counts back to 5 customers, 8 goods, 0 transactions.
- [x] **Step 3: Update README + plan checkboxes**
- [x] **Step 4: Commit** `feat: phase 3 transaksi penjualan` and stop before Phase 4.
