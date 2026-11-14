# Phase 5 â€” Laporan Design

**Date:** 2026-11-14  
**Status:** Implemented
**Chosen approach:** Shared filter+query logic per report feeding (1) Thymeleaf HTML preview, (2) Apache POI Excel export, (3) JasperReports PDF export with DB letterhead; print via browser print of preview/PDF.

## Intent
Provide 4 sales reports with consistent filtering and export/print, using real DB data and institution letterhead from `identitas_instansi`.

## 4 Reports (adopted)
All filters are inclusive on dates (`tanggal >= startOfDay(from)` and `tanggal < startOfDay(to)+1d`). "Seluruh baris hasil filter" exported (no pagination in exports).

1. **Laporan Transaksi** (`/laporan/transaksi`)
   - Filters: tanggal dariâ€“sampai (required), pelanggan (opsional).
   - Columns: No, Nomor Transaksi, Tanggal, Pelanggan, Kasir, Total. Footer/summary: jumlah transaksi, total omzet.

2. **Laporan Detail Penjualan** (`/laporan/detail-penjualan`)
   - Filters: tanggal dariâ€“sampai (required), pelanggan (opsional), nomor/kata kunci transaksi (opsional, matches nomor/id text) â€” satisfies "customer/transaction filters" relevance.
   - Columns: No, Nomor Transaksi, Tanggal, Pelanggan, Barang, Jumlah, Harga Satuan (snapshot), Subtotal. Summary: total qty, total nilai.

3. **Laporan Penjualan per Barang** (`/laporan/penjualan-barang`)
   - Filters: tanggal dariâ€“sampai (required).
   - Columns: No, Kode Barang, Nama Barang, Satuan, Qty Terjual, Total Nilai (sum subtotal). Summary totals.

4. **Laporan Penjualan per Pelanggan** (`/laporan/penjualan-pelanggan`)
   - Filters: tanggal dariâ€“sampai (required), pelanggan (opsional).
   - Columns: No, Nama Pelanggan, Jumlah Transaksi, Total Omzet. Summary totals.

## Goals / Requirements
- Each report: filter form + HTML preview table, buttons **Tampilkan**, **Excel**, **PDF**, **Cetak**.
- Excel: `.xlsx` via Apache POI, includes letterhead info (instansi name/address/contact) and title/period, all filtered rows.
- PDF: via JasperReports (`.jrxml` templates), letterhead from `identitas_instansi` DB row, all filtered rows.
- Print: printable view (or print PDF/preview with print CSS) showing letterhead and all filtered rows.
- No fake data; empty results show clear empty state.

## Constraints
- Java 21 / Spring Boot 4.1.2, Thymeleaf, MySQL 8.4.3. No Docker. No schema changes.
- Add dependencies: Apache POI (`poi-ooxml`) and JasperReports (+ fonts if needed). Keep versions compatible with Boot 4/Jakarta.
- Keep Phase 0â€“4 working. Enable **Laporan** nav (replace "Segera") linking to report index/menu.

## Implementation Shape
- `LaporanController` routes: index `/laporan`, per-report GET pages, and export endpoints (e.g. `/laporan/.../excel`, `/laporan/.../pdf`) accepting same filter params.
- Query/repository methods (JPQL/native as needed) for each report with date range + optional filters, ordered deterministically; exports stream full result.
- DTO/projection objects for aggregated reports (per barang, per pelanggan).
- `src/main/resources/reports/*.jrxml` compiled by Jasper at runtime/build; shared letterhead parameters (nama, alamat, email, telepon, periode, judul).
- Excel builder utility per report (or shared helper) writing header + rows + summary.
- Templates: `laporan/index.html`, `laporan/transaksi.html`, `laporan/detail-penjualan.html`, `laporan/penjualan-barang.html`, `laporan/penjualan-pelanggan.html` (filter form + preview + action buttons).
- Tests: query/filter correctness and controller/export content-type/basic content for representative cases; full suite green.

## Verification
- `.\mvnw.cmd test` green. Browser: each report preview with date filter returns correct rows, Excel downloads/openable, PDF downloads with DB letterhead, print works. Commit, stop before audit/UI pass. Stop before no further phases remain after 5.
