CREATE TABLE barang (
    id_barang BIGINT NOT NULL AUTO_INCREMENT,
    kode_barang VARCHAR(20) NOT NULL,
    nama_barang VARCHAR(100) NOT NULL,
    satuan VARCHAR(20) NOT NULL DEFAULT 'pcs',
    harga DECIMAL(15, 2) NOT NULL,
    stok INT NOT NULL DEFAULT 0,
    stok_minimum INT NOT NULL DEFAULT 5,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_barang),
    UNIQUE KEY uk_barang_kode (kode_barang),
    KEY idx_barang_nama (nama_barang),
    CONSTRAINT chk_barang_harga CHECK (harga >= 0),
    CONSTRAINT chk_barang_stok CHECK (stok >= 0),
    CONSTRAINT chk_barang_stok_minimum CHECK (stok_minimum >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
