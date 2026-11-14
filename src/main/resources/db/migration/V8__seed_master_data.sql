-- Demonstration seed data for local development.
INSERT INTO identitas_instansi (nama_instansi, alamat, email, no_telepon, logo)
VALUES ('Toko Sinar Jaya', 'Jl. Merdeka No. 10, Bandung, Jawa Barat', 'info@sinarjaya.local', '081234567890', NULL);

INSERT INTO pelanggan (nama, alamat, no_telepon, email) VALUES
    ('Budi Santoso', 'Jl. Cihampelas No. 21, Bandung', '081234567801', 'budi@example.com'),
    ('Siti Aminah', 'Jl. Dago No. 8, Bandung', '081234567802', 'siti@example.com'),
    ('Andi Wijaya', 'Jl. Riau No. 45, Bandung', '081234567803', 'andi@example.com'),
    ('Dewi Lestari', 'Jl. Sukajadi No. 12, Bandung', '081234567804', 'dewi@example.com'),
    ('Rudi Hartono', 'Jl. Setiabudi No. 99, Bandung', '081234567805', 'rudi@example.com');

INSERT INTO barang (kode_barang, nama_barang, satuan, harga, stok, stok_minimum) VALUES
    ('BRG-001', 'Kertas HVS A4 80gsm', 'rim', 55000.00, 40, 10),
    ('BRG-002', 'Pulpen Gel Hitam', 'pcs', 4500.00, 120, 20),
    ('BRG-003', 'Map Plastik A4', 'pcs', 2500.00, 80, 15),
    ('BRG-004', 'Stapler Medium', 'pcs', 32000.00, 15, 5),
    ('BRG-005', 'Isi Staples No. 10', 'kotak', 3500.00, 60, 10),
    ('BRG-006', 'Lem Kertas 20gr', 'pcs', 8000.00, 8, 10),
    ('BRG-007', 'Spidol Whiteboard', 'pcs', 12000.00, 25, 8),
    ('BRG-008', 'Buku Tulis 38 Lembar', 'pcs', 6000.00, 100, 20);
