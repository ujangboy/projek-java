package com.example.penjualan.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.penjualan.entity.Transaksi;

public interface TransaksiRepository extends JpaRepository<Transaksi, Long> {

	@EntityGraph(attributePaths = { "pelanggan", "user" })
	Page<Transaksi> findAllByOrderByTanggalDescIdTransaksiDesc(Pageable pageable);

	@EntityGraph(attributePaths = { "pelanggan", "user", "details", "details.barang" })
	@Query("SELECT t FROM Transaksi t WHERE t.idTransaksi = :id")
	Optional<Transaksi> findDetailById(@Param("id") Long id);

	@Query("SELECT COALESCE(SUM(t.total), 0) FROM Transaksi t WHERE t.tanggal >= :start AND t.tanggal < :end")
	BigDecimal sumTotalBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	@Query("SELECT COALESCE(SUM(t.total), 0) FROM Transaksi t")
	BigDecimal sumTotalAll();

	@EntityGraph(attributePaths = { "pelanggan", "user" })
	@Query("""
			SELECT t FROM Transaksi t
			WHERE t.tanggal >= :start AND t.tanggal < :end
			  AND (:idPelanggan IS NULL OR t.pelanggan.idPelanggan = :idPelanggan)
			ORDER BY t.tanggal DESC, t.idTransaksi DESC
			""")
	List<Transaksi> findReportTransaksi(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
			@Param("idPelanggan") Long idPelanggan);

	@Query("""
			SELECT b.idBarang AS idBarang, b.kodeBarang AS kodeBarang, b.namaBarang AS namaBarang, b.satuan AS satuan,
			       COALESCE(SUM(d.jumlah), 0) AS qty, COALESCE(SUM(d.subtotal), 0) AS nilai
			FROM DetailTransaksi d
			JOIN d.transaksi t
			JOIN d.barang b
			WHERE t.tanggal >= :start AND t.tanggal < :end
			GROUP BY b.idBarang, b.kodeBarang, b.namaBarang, b.satuan
			ORDER BY nilai DESC, b.namaBarang ASC
			""")
	List<BarangSalesReport> findSalesByBarang(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

	@Query("""
			SELECT p.idPelanggan AS idPelanggan, p.nama AS nama,
			       COUNT(t) AS jumlahTransaksi, COALESCE(SUM(t.total), 0) AS totalOmzet
			FROM Transaksi t
			JOIN t.pelanggan p
			WHERE t.tanggal >= :start AND t.tanggal < :end
			  AND (:idPelanggan IS NULL OR p.idPelanggan = :idPelanggan)
			GROUP BY p.idPelanggan, p.nama
			ORDER BY totalOmzet DESC, p.nama ASC
			""")
	List<PelangganSalesReport> findSalesByPelanggan(@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end, @Param("idPelanggan") Long idPelanggan);

	interface BarangSalesReport {
		Long getIdBarang();
		String getKodeBarang();
		String getNamaBarang();
		String getSatuan();
		Long getQty();
		BigDecimal getNilai();
	}

	interface PelangganSalesReport {
		Long getIdPelanggan();
		String getNama();
		Long getJumlahTransaksi();
		BigDecimal getTotalOmzet();
	}
}
