# Phase 4 — Dashboard & Statistik Design

**Date:** 2026-11-14  
**Status:** Proposed (awaiting approval)  
**Chosen approach:** Option 1 — repository aggregation queries assembled in `DashboardController` (consistent with Phase 2 style, minimal new layers).

## Intent & Why
Replace the Phase 0 placeholder dashboard with a live summary so the admin immediately sees business volume, revenue, recent activity, and stock that needs attention. All figures must come from the database (no dummy/placeholder values).

## Goals
- Show 6 summary cards with real DB values:
  - **Total Pelanggan** — count of `pelanggan`.
  - **Total Barang** — count of `barang`.
  - **Total Transaksi** — count of `transaksi` (all time).
  - **Omzet Hari Ini** — sum of `transaksi.total` for transactions whose `tanggal` falls on today (server/local date).
  - **Omzet Bulan Ini** — sum of `transaksi.total` for transactions in the current calendar month.
  - **Omzet Total** — sum of `transaksi.total` over all transactions.
- Show **Transaksi Terbaru** table: 5 most recent transactions (nomor/tanggal, pelanggan, kasir, total) linking to detail.
- Show **Stok Menipis** table: items where `stok <= stok_minimum`, ordered to surface most urgent first (e.g. by `stok` asc, then name), showing kode, nama, satuan, stok, stok_minimum.
- Empty states for the tables when there is no data (no fake rows).

## Non-goals / Anti-goals
- No charts/graphs, date pickers, or configurable periods (not requested).
- No changes to Phase 0–3 behavior, routes, or schema/migrations.
- No fake statistics or hardcoded numbers.
- UI/UX visual polish pass is deferred to the later “audit + UI/UX adjustment” step the user requested (this phase delivers correct, clear, usable data presentation).

## Constraints
- Java 21 / Spring Boot 4.1.2, Thymeleaf, Bootstrap 5 WebJars, MySQL 8.4.3 (Laragon). No Docker.
- Periods: “hari ini” = `[startOfToday, startOfTomorrow)`; “bulan ini” = `[startOfMonth, startOfNextMonth)` using `LocalDate.now()` / `LocalDateTime` boundaries in application time. `SUM` over no rows must render as `Rp 0,00` (treat null as zero).
- Keep existing layout/nav patterns and `activeMenu = "dashboard"`.

## Approaches Considered
1. **Repository aggregation in controller (chosen).** Add focused `count/sum/query` methods to existing repositories and assemble the model in `DashboardController`. Simple, consistent with Phase 2, easy to test.
2. **`DashboardService`.** Cleaner if metrics grow, but extra layer for a single read-only page now — unnecessary.
3. **Single large aggregate query/projection.** Fewer round-trips but harder to read/test; negligible benefit here.

## Parts Decided (implementation shape)
- **`TransaksiRepository`**: methods for omzet sums by date range (e.g. `@Query` summing `t.total` where `t.tanggal >= :start and t.tanggal < :end`, returning `BigDecimal`/nullable) and for total omzet; reuse existing paging for recent list (`findAllByOrderByTanggalDescIdTransaksiDesc(PageRequest.of(0,5))`) or add a dedicated top-5 query with needed graph.
- **`BarangRepository`**: method to find low-stock items (`b.stok <= b.stokMinimum`) with sensible ordering.
- **`PelangganRepository`**: use inherited `count()` (no change unless needed).
- **`DashboardController`**: inject repositories, compute today/month boundaries, load all 6 metrics + recent transactions + low-stock list, add to model, return `dashboard/index`.
- **`dashboard/index.html`**: replace placeholder with card grid (6 cards) + “Transaksi Terbaru” table + “Stok Menipis” table, formatted amounts `Rp …`, empty-state messages, links to transaction detail/list and barang list where useful. Update eyebrow/heading copy to reflect live dashboard (remove “Phase 0/Berikutnya” placeholder text).
- **Tests (`Phase4DashboardTest`)**: seed known pelanggan/barang/transaksi rows in setup, assert `/dashboard` shows expected counts/sums (today, this month, total), includes recent transaction and low-stock item, and shows `Rp 0,00` / empty states when appropriate; clean up fixtures.

## Left to the Builder
- Exact JPQL signatures/null-handling helpers and minor view layout details (card arrangement, column labels) consistent with existing Bootstrap/app styles.
- Commit after tests + browser verification, then stop before Phase 5.

## Verification Plan
- `.\mvnw.cmd test` (Phase 0–4 green).
- Browser: login, open dashboard, confirm cards match DB, recent and low-stock sections render correctly/empty states; take screenshot. Clean any throwaway DB changes if used outside tests. Stop before Phase 5.