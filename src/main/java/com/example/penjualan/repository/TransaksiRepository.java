package com.example.penjualan.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
}
