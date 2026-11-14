# Sistem Informasi Penjualan

Aplikasi web penjualan lokal berbasis Java Spring Boot + MySQL Laragon. Tanpa Docker.

## Status

Phase 2 — Data Master: CRUD pelanggan dan barang dengan pencarian, pagination, validasi server-side, dan proteksi Foreign Key.

| Tabel | Isi |
|---|---|
| `users` | akun admin |
| `pelanggan` | data pelanggan |
| `barang` | data barang dan stok |
| `transaksi` | header transaksi penjualan |
| `detail_transaksi` | rincian barang per transaksi |
| `identitas_instansi` | identitas kop surat |

## Prasyarat

- JDK 21 atau lebih baru (proyek ini dikompilasi untuk Java 21, diuji pada JDK 26).
- MySQL Laragon 8.4.x berjalan di `127.0.0.1:3306`.
- Tidak perlu Maven global; gunakan `mvnw.cmd`.

## Menjalankan

1. Pastikan MySQL Laragon menyala.
2. Siapkan database (sekali saja):

   ```sql
   CREATE DATABASE IF NOT EXISTS db_sistem_penjualan
     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. Jalankan aplikasi:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Buka <http://localhost:8080> lalu masuk dengan `admin` / `admin123`.

Kredensial database default: user `root`, password kosong. Ubah lewat environment variable `DB_USERNAME` dan `DB_PASSWORD`.

## Perintah

```powershell
.\mvnw.cmd test          # jalankan test
.\mvnw.cmd clean package # build jar
```

## Struktur singkat

- `src/main/java/com/example/penjualan` — kode aplikasi.
- `src/main/resources/db/migration` — migrasi Flyway.
- `src/main/resources/templates` — halaman Thymeleaf.
- `src/main/resources/static` — CSS.
- `docs/superpowers/plans` — rencana per fase.
