CREATE TABLE transaksi (
    id_transaksi BIGINT NOT NULL AUTO_INCREMENT,
    id_pelanggan BIGINT NOT NULL,
    id_user BIGINT NOT NULL,
    tanggal DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id_transaksi),
    KEY idx_transaksi_tanggal (tanggal),
    KEY idx_transaksi_pelanggan (id_pelanggan),
    KEY idx_transaksi_user (id_user),
    CONSTRAINT fk_transaksi_pelanggan FOREIGN KEY (id_pelanggan) REFERENCES pelanggan (id_pelanggan)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_transaksi_user FOREIGN KEY (id_user) REFERENCES users (id_user)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_transaksi_total CHECK (total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
