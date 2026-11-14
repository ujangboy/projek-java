CREATE TABLE detail_transaksi (
    id_detail BIGINT NOT NULL AUTO_INCREMENT,
    id_transaksi BIGINT NOT NULL,
    id_barang BIGINT NOT NULL,
    jumlah INT NOT NULL,
    harga_satuan DECIMAL(15, 2) NOT NULL,
    subtotal DECIMAL(15, 2) NOT NULL,
    PRIMARY KEY (id_detail),
    KEY idx_detail_transaksi (id_transaksi),
    KEY idx_detail_barang (id_barang),
    CONSTRAINT fk_detail_transaksi FOREIGN KEY (id_transaksi) REFERENCES transaksi (id_transaksi)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_detail_barang FOREIGN KEY (id_barang) REFERENCES barang (id_barang)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_detail_jumlah CHECK (jumlah > 0),
    CONSTRAINT chk_detail_harga_satuan CHECK (harga_satuan >= 0),
    CONSTRAINT chk_detail_subtotal CHECK (subtotal >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
