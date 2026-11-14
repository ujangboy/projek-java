package com.example.penjualan.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.penjualan.entity.DetailTransaksi;

public interface DetailTransaksiRepository extends JpaRepository<DetailTransaksi, Long> {

	@Query("""
			SELECT d FROM DetailTransaksi d
			JOIN FETCH d.transaksi t
			JOIN FETCH t.pelanggan p
			JOIN FETCH d.barang b
			WHERE t.tanggal >= :start AND t.tanggal < :end
			  AND (:idPelanggan IS NULL OR p.idPelanggan = :idPelanggan)
			  AND (:q IS NULL OR :q = ''
			       OR CAST(t.idTransaksi AS string) LIKE CONCAT('%', :q, '%')
			       OR LOWER(p.nama) LIKE LOWER(CONCAT('%', :q, '%')))
			ORDER BY t.tanggal DESC, t.idTransaksi DESC, d.idDetail ASC
			""")
	List<DetailTransaksi> findReportDetails(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
			@Param("idPelanggan") Long idPelanggan, @Param("q") String q);
}