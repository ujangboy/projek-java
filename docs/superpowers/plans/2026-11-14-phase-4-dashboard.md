# Phase 4 â€” Dashboard & Statistik Implementation Plan

> **Spec:** `docs/superpowers/specs/2026-11-14-phase-4-dashboard-design.md` (approved). Approach 1: repository aggregation in controller.

**Goal:** Live dashboard with 6 DB-backed cards, 5 recent transactions, and low-stock list.

## Tasks

- [x] **Task 1 (RED):** Add `Phase4DashboardTest` seeding fixtures and asserting `/dashboard` shows counts, omzet hari ini/bulan ini/total, recent transaction, low-stock item, and `Rp 0,00`/empty states where applicable. Run targeted test to confirm failure.
- [x] **Task 2 (GREEN):** Add repo methods: `TransaksiRepository.sumTotalBetween(start,end)` and `sumTotalAll()` (null-safe in controller), recent 5 with `pelanggan`/`user` graph; `BarangRepository.findLowStock()` (`stok <= stokMinimum`). Wire `DashboardController` (inject repos, compute today/month boundaries, zero-safe sums) and replace `dashboard/index.html` with cards + tables + empty states.
- [x] **Task 3:** Run `Phase4DashboardTest` then full `mvnw.cmd test`. Browser-verify dashboard, clean DB, update README/plan, commit `feat: phase 4 dashboard`.

## Notes
- Omzet = sum `transaksi.total`; null sum -> `BigDecimal.ZERO`.
- "Hari ini": `[LocalDate.now().atStartOfDay(), tomorrowStart)`. "Bulan ini": `[firstOfMonth.atStartOfDay(), firstOfNextMonth.atStartOfDay())`.
- No schema/Phase 0â€“3 behavior changes. Stop before Phase 5.
